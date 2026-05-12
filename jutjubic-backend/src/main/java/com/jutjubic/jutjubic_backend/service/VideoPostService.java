package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.dto.CreateVideoPostRequestDto;
import com.jutjubic.jutjubic_backend.dto.TranscodingRequest;
import com.jutjubic.jutjubic_backend.dto.UploadEvent;
import com.jutjubic.jutjubic_backend.dto.VideoPostDto;
import com.jutjubic.jutjubic_backend.exception.ApiExcepiton;
import com.jutjubic.jutjubic_backend.messaging.UploadEventPublisher;
import com.jutjubic.jutjubic_backend.model.*;
import com.jutjubic.jutjubic_backend.repository.TagRepository;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jutjubic.jutjubic_backend.repository.VideoPostLikeRepository;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class VideoPostService {

    private final VideoPostRepository postRepo;
    private final TagRepository tagRepo;
    private final FileStorageService storage;
    private final VideoPostLikeRepository likeRepo;
    private final UserRepository userRepo;
    private final ViewCounterService viewCounterService;
    private final MapTileCacheService tileCache;
    private final MapTileInvalidationService tileInvalidation;
    private final HLSService hlsService;
    private final TranscodingProducer transcodingProducer;

    private static final long MAX_VIDEO_BYTES = 200L * 1024 * 1024; // 200MB

    private final UploadEventPublisher uploadEventPublisher;


    @Transactional
    public VideoPost create(CreateVideoPostRequestDto dto,
                            MultipartFile thumbnail,
                            MultipartFile video,
                            User author) {

        String thumbPath = null;
        String videoPath = null;

        try {
            log.info("Starting video creation for user: {}", author.getEmail());

            if (video == null || video.isEmpty()) {
                throw new ApiExcepiton("Video file is required.");
            }

            if (video.getSize() > MAX_VIDEO_BYTES) {
                throw new ApiExcepiton("Video is too large. Max size is 200MB.");
            }

            String contentType = video.getContentType();
            if (contentType == null || !contentType.equalsIgnoreCase("video/mp4")) {
                throw new ApiExcepiton("Video must be an MP4 file.");
            }

            log.info("Saving files...");
            thumbPath = storage.saveWithTimeout(thumbnail, "thumbnails", guessExt(thumbnail));
            log.info("Thumbnail saved: {}", thumbPath);

            videoPath = storage.saveWithTimeout(video, "videos", guessExt(video));
            log.info("Video saved: {}", videoPath);

            Set<Tag> resolvedTags = new HashSet<>();
            if (dto.getTags() != null) {
                for (String raw : dto.getTags()) {
                    if (raw == null || raw.isBlank()) continue;

                    String name = raw.trim().toLowerCase();

                    Tag tag = tagRepo.findByNameIgnoreCase(name)
                            .orElseGet(() -> {
                                Tag t = new Tag();
                                t.setName(name);
                                return tagRepo.save(t);
                            });

                    resolvedTags.add(tag);
                }
            }
            if (resolvedTags.isEmpty()) {
                throw new ApiExcepiton("At least one valid tag is required.");
            }

            GeoLocation location = null;
            if (dto.getLatitude() != null && dto.getLongitude() != null) {
                location = new GeoLocation();
                location.setLatitude(dto.getLatitude());
                location.setLongitude(dto.getLongitude());
            }

            VideoPost post = new VideoPost();
            post.setTitle(dto.getTitle());
            post.setDescription(dto.getDescription());
            post.setTags(resolvedTags);
            post.setLocation(location);
            post.setThumbnailPath(thumbPath);
            post.setVideoPath(videoPath);
            post.setCreatedAt(LocalDateTime.now());
            post.setAuthor(author);

            // ===== SCHEDULED VIDEO LOGIC =====
            if (dto.getScheduledAt() != null) {
                LocalDateTime scheduledTime = dto.getScheduledAt();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime minimumTime = now.plusSeconds(10);

                log.info("Processing scheduled video. Scheduled: {}, Now: {}, Minimum: {}",
                        scheduledTime, now, minimumTime);

                if (scheduledTime.isBefore(minimumTime)) {
                    log.warn("Scheduled time {} is before minimum time {}. Now: {}",
                            scheduledTime, minimumTime, now);
                    throw new ApiExcepiton("Scheduled time must be in the future (at least 10 seconds from now).");
                }

                post.setScheduled(true);
                post.setScheduledAt(scheduledTime);
                log.info("Video scheduled for: {} (current time: {})", scheduledTime, now);
            } else {
                log.info("Creating regular (non-scheduled) video");
            }

            // Get video duration with error handling
            log.info("Getting video duration for: {}", videoPath);
            try {
                int duration = hlsService.getVideoDuration(videoPath);
                post.setVideoDurationSeconds(duration);
                log.info("Video duration: {} seconds", duration);
            } catch (Exception e) {
                log.error("Failed to get video duration, setting to 0", e);
                post.setVideoDurationSeconds(0);
            }
            // ===== END SCHEDULED VIDEO LOGIC =====

            // ===== SAVE VIDEO =====
            log.info("Saving video post to database...");
            VideoPost saved = postRepo.save(post);


            var event = UploadEvent.builder()
                    .videoId(saved.getId())
                    .title(saved.getTitle())
                    .authorEmail(author.getEmail())
                    .sizeBytes(video.getSize())
                    .createdAt(saved.getCreatedAt().toString())
                    .build();

            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            uploadEventPublisher.publishBoth(event);
                        }
                    }
            );





            log.info("Video post saved with ID: {}", saved.getId());

            // ===== ASYNC TRANSCODING =====
            log.info("Starting async transcoding thread...");
            new Thread(() -> {
                try {
                    log.info("Sleeping 2 seconds before sending to queue...");
                    Thread.sleep(2000);

                    log.info("Sending video ID {} to transcoding queue...", saved.getId());
                    transcodingProducer.sendForTranscoding(
                            new TranscodingRequest(saved.getId(), saved.getVideoPath())
                    );

                    log.info("Transcoding request sent successfully for video ID: {}", saved.getId());

                } catch (InterruptedException e) {
                    log.error("Transcoding thread interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Error sending transcoding request", e);
                }
            }).start();

            System.out.println("🔵 Processing tile invalidation...");
            if (saved.getLocation() != null) {
                log.info("Invalidating tile cache for location: {}, {}",
                        saved.getLocation().getLatitude(), saved.getLocation().getLongitude());
                tileInvalidation.invalidateForNewVideo(
                        saved.getLocation().getLatitude(),
                        saved.getLocation().getLongitude()
                );
            }

            // ===== ASYNC HLS CONVERSION =====
            // Run async - don't block upload if it fails
            log.info("Scheduling HLS conversion for post ID: {}", saved.getId());
            try {
                convertToHLSAsync(saved.getId(), videoPath);
                log.info("HLS conversion scheduled successfully for post ID: {}", saved.getId());
            } catch (Exception e) {
                log.error("Failed to schedule HLS conversion for post ID: {}", saved.getId(), e);
                // Don't throw - upload should succeed even if HLS fails
            }
            // ===== END HLS CONVERSION =====

            log.info("Video creation completed successfully. Post ID: {}", saved.getId());
            log.info("Returning saved post with ID: {}", saved.getId());
            return saved;

        } catch (ApiExcepiton ex) {
            log.error("API Exception during video creation: {}", ex.getMessage());
            storage.deleteIfExists(thumbPath);
            storage.deleteIfExists(videoPath);
            throw ex;

        } catch (Exception ex) {
            log.error("Unexpected error during video creation", ex);
            storage.deleteIfExists(thumbPath);
            storage.deleteIfExists(videoPath);
            throw new ApiExcepiton("Failed to create video: " + ex.getMessage());
        }
    }


    @Async
    public void convertToHLSAsync(Long postId, String videoPath) {
        log.info("=== STARTING ASYNC HLS CONVERSION ===");
        log.info("Post ID: {}", postId);
        log.info("Video Path: {}", videoPath);
        log.info("Thread: {}", Thread.currentThread().getName());

        try {
            log.info("Starting HLS conversion for post ID: {}", postId);

            String hlsPlaylistPath = hlsService.convertToHLS(videoPath);
            log.info("HLS conversion completed. Playlist path: {}", hlsPlaylistPath);

            // Use new transaction for updating the post
            updatePostWithHLS(postId, hlsPlaylistPath);

            log.info("=== HLS CONVERSION COMPLETED SUCCESSFULLY ===");

        } catch (Exception e) {
            log.error("=== HLS CONVERSION FAILED ===", e);
            log.error("Post ID: {}", postId);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());

            // Mark as failed but don't throw - video is still accessible via MP4
            try {
                markHLSAsFailed(postId);
            } catch (Exception e2) {
                log.error("Failed to mark HLS as failed for post ID: {}", postId, e2);
            }
        }
    }

    @Transactional
    public void updatePostWithHLS(Long postId, String hlsPlaylistPath) {
        log.info("Updating post {} with HLS playlist: {}", postId, hlsPlaylistPath);
        VideoPost post = postRepo.findById(postId)
                .orElseThrow(() -> new ApiExcepiton("Post not found"));

        post.setHlsPlaylistPath(hlsPlaylistPath);
        post.setHlsProcessed(true);
        postRepo.save(post);
        log.info("Post updated successfully with HLS data");
    }

    @Transactional
    public void markHLSAsFailed(Long postId) {
        log.info("Marking HLS as failed for post ID: {}", postId);
        VideoPost post = postRepo.findById(postId).orElse(null);
        if (post != null) {
            post.setHlsProcessed(false);
            postRepo.save(post);
        }
    }

    private String guessExt(MultipartFile f) {
        if (f == null || f.getOriginalFilename() == null) return "";
        int idx = f.getOriginalFilename().lastIndexOf('.');
        return idx == -1 ? "" : f.getOriginalFilename().substring(idx);
    }

    @Transactional(readOnly = true)
    public List<VideoPostDto> getAll(Authentication auth) {
        log.info("Getting all videos");
        List<VideoPost> posts = postRepo.findAllByOrderByCreatedAtDesc();

        LocalDateTime now = LocalDateTime.now();
        posts = posts.stream()
                .filter(p -> !p.isScheduled() || !now.isBefore(p.getScheduledAt()))
                .toList();

        log.info("Found {} videos (after filtering scheduled)", posts.size());
        return posts.stream()
                .map(p -> enrichLikes(p, auth))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VideoPostDto> getMine(Long authorId, Authentication auth) {
        log.info("Getting videos for author ID: {}", authorId);
        return postRepo.findByAuthor_IdOrderByCreatedAtDesc(authorId)
                .stream()
                .map(p -> enrichLikes(p, auth))
                .toList();
    }

    @Transactional(readOnly = true)
    public VideoPostDto getById(Long id, Authentication auth) {
        log.info("Getting video by ID: {}", id);
        VideoPost post = postRepo.findWithTagsById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (post.isScheduled() && LocalDateTime.now().isBefore(post.getScheduledAt())) {
            Long userId = resolveUserId(auth);
            if (userId == null || !post.getAuthor().getId().equals(userId)) {
                log.warn("Attempt to access scheduled video {} by non-owner", id);
                throw new ApiExcepiton("This video is scheduled and not yet available.");
            }
        }

        return enrichLikes(post, auth);
    }

    @Cacheable(value = "thumbnails", key = "'thumb:' + #postId", unless = "#result == null")
    @Transactional(readOnly = true)
    public byte[] getThumbnailBytes(Long postId) {
        VideoPost post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        return storage.readBytes(post.getThumbnailPath());
    }

    @Transactional(readOnly = true)
    public byte[] getVideoBytes(Long postId) {
        VideoPost post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (post.isScheduled() && LocalDateTime.now().isBefore(post.getScheduledAt())) {
            throw new ApiExcepiton("This video is not yet available.");
        }

        return storage.readBytes(post.getVideoPath());
    }

    @Transactional(readOnly = true)
    public String getThumbnailPath(Long postId) {
        VideoPost post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return post.getThumbnailPath();
    }

    public String getHLSPlaylist(Long id, Authentication auth) {
        log.info("Getting HLS playlist for video ID: {}", id);
        VideoPost post = postRepo.findById(id)
                .orElseThrow(() -> new ApiExcepiton("Video not found"));

        // Frontend now checks scheduled status, no need to block here

        // Only check if HLS is ready
        if (!post.isHlsProcessed() || post.getHlsPlaylistPath() == null) {
            log.warn("HLS not ready for video ID: {}", id);
            throw new ApiExcepiton("HLS stream not yet ready. Please try again later.");
        }

        log.info("Returning HLS playlist: {}", post.getHlsPlaylistPath());
        return post.getHlsPlaylistPath();
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;

        return userRepo.findByEmail(auth.getName())
                .or(() -> userRepo.findByUsername(auth.getName()))
                .map(User::getId)
                .orElse(null);
    }

    private VideoPostDto enrichLikes(VideoPost post, Authentication auth) {
        VideoPostDto dto = VideoPostDto.from(post);

        long count = likeRepo.countByVideoPost_Id(post.getId());
        dto.setLikesCount(count);

        Long userId = resolveUserId(auth);
        boolean likedByMe = (userId != null) && likeRepo.existsByVideoPost_IdAndUser_Id(post.getId(), userId);
        dto.setLikedByMe(likedByMe);

        dto.setViewsCount(post.getViewsCount());

        return dto;
    }

    @Transactional
    public LikeResponse toggleLike(Long postId, Authentication auth) {
        Long userId = resolveUserId(auth);
        if (userId == null) {
            throw new RuntimeException("Unauthorized");
        }

        VideoPost post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        boolean exists = likeRepo.existsByVideoPost_IdAndUser_Id(postId, userId);

        if (exists) {
            likeRepo.deleteByVideoPost_IdAndUser_Id(postId, userId);
        } else {
            User user = userRepo.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            var like = new com.jutjubic.jutjubic_backend.model.VideoPostLike();
            like.setUser(user);
            like.setVideoPost(post);
            likeRepo.save(like);
        }

        long count = likeRepo.countByVideoPost_Id(postId);
        boolean likedByMe = likeRepo.existsByVideoPost_IdAndUser_Id(postId, userId);

        return new LikeResponse(count, likedByMe);
    }

    public VideoAvailabilityStatus checkVideoAvailability(Long id, Authentication auth) {
        VideoPost post = postRepo.findById(id)
                .orElseThrow(() -> new ApiExcepiton("Video not found"));

        Long userId = resolveUserId(auth);
        boolean isOwner = userId != null && post.getAuthor().getId().equals(userId);

        // Owner can always access
        if (isOwner) {
            return new VideoAvailabilityStatus(true, true, null, null);
        }

        // Check if scheduled and not yet started
        if (post.isScheduled()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime scheduledTime = post.getScheduledAt();

            if (now.isBefore(scheduledTime)) {
                // Not yet available
                return new VideoAvailabilityStatus(false, false, scheduledTime, null);
            } else {
                // Live now
                return new VideoAvailabilityStatus(true, false, scheduledTime, now);
            }
        }

        // Regular video
        return new VideoAvailabilityStatus(true, true, null, null);
    }

    public record VideoAvailabilityStatus(
            boolean available,
            boolean isOwner,
            LocalDateTime scheduledAt,
            LocalDateTime currentTime
    ) {}

    public record LikeResponse(long likesCount, boolean likedByMe) {}

    public void incrementView(Long id) {
        viewCounterService.incrementAndGet(id);
    }


    public String getHLSSegmentPath(Long id, String filename) {
        VideoPost post = postRepo.findById(id)
                .orElseThrow(() -> new ApiExcepiton("Video not found"));

        if (!post.isHlsProcessed() || post.getHlsPlaylistPath() == null) {
            throw new ApiExcepiton("HLS not ready");
        }

        String playlistPath = post.getHlsPlaylistPath();
        String hlsDir = playlistPath.substring(0, playlistPath.lastIndexOf('/'));

        return hlsDir + "/" + filename;
    }
}