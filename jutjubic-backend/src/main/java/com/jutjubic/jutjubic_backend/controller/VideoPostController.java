package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.CreateVideoPostRequestDto;
import com.jutjubic.jutjubic_backend.dto.VideoPostDto;
import com.jutjubic.jutjubic_backend.exception.ApiExcepiton;
import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.service.FileStorageService;
import com.jutjubic.jutjubic_backend.service.VideoPostService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/posts")
public class VideoPostController {

    private final VideoPostService service;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public VideoPostController(VideoPostService service, UserRepository userRepository, FileStorageService fileStorageService) {
        this.service = service;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    private MediaType imageTypeFromPath(String path) {
        if (path == null) return MediaType.IMAGE_JPEG;

        String p = path.toLowerCase();
        if (p.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;

        return MediaType.APPLICATION_OCTET_STREAM;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> create(
            @Valid @RequestPart("metadata") CreateVideoPostRequestDto metadata,
            @RequestPart("thumbnail") MultipartFile thumbnail,
            @RequestPart("video") MultipartFile video,
            Authentication authentication
    ) {
        try {
            String email = authentication.getName();

            User author = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            VideoPost created = service.create(metadata, thumbnail, video, author);

            return ResponseEntity.ok(created.getId());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<VideoPostDto>> getAll(Authentication authentication) {
        return ResponseEntity.ok(service.getAll(authentication));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<VideoPostDto>> getMine(Authentication authentication) {
        String principal = authentication.getName();

        User me = userRepository.findByEmail(principal)
                .or(() -> userRepository.findByUsername(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(service.getMine(me.getId(), authentication));
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id) {
        String path = service.getThumbnailPath(id);
        byte[] bytes = service.getThumbnailBytes(id);

        return ResponseEntity.ok()
                .contentType(imageTypeFromPath(path))
                .body(bytes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoPostDto> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(service.getById(id, authentication));
    }

    @GetMapping(value = "/{id}/video", produces = "video/mp4")
    public ResponseEntity<byte[]> getVideo(@PathVariable Long id) {
        service.incrementView(id);

        byte[] bytes = service.getVideoBytes(id);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("video/mp4"))
                .body(bytes);
    }

    @GetMapping(value = "/{id}/hls/playlist.m3u8", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<Resource> getHLSPlaylist(@PathVariable Long id, Authentication authentication) {
        service.incrementView(id);

        try {
            log.info("=== HLS PLAYLIST REQUEST ===");
            log.info("Video ID: {}", id);

            String playlistPath = service.getHLSPlaylist(id, authentication);
            log.info("Playlist path from service: {}", playlistPath);

            Path absolutePath = fileStorageService.resolveAbsolute(playlistPath);
            log.info("Resolved absolute path: {}", absolutePath);
            log.info("File exists: {}", Files.exists(absolutePath));

            if (!Files.exists(absolutePath)) {
                log.error("Playlist file does NOT exist at: {}", absolutePath);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            long fileSize = Files.size(absolutePath);
            log.info("File size: {} bytes", fileSize);

            Resource resource = new FileSystemResource(absolutePath);
            log.info("Returning playlist resource");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource);

        } catch (Exception e) {
            log.error("=== HLS PLAYLIST ERROR ===", e);
            log.error("Video ID: {}", id);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/{id}/hls/{filename:.+}", produces = "video/mp2t")
    public ResponseEntity<Resource> getHLSSegment(
            @PathVariable Long id,
            @PathVariable String filename) {

        log.info("=== HLS SEGMENT REQUEST ===");
        log.info("Video ID: {}, Filename: {}", id, filename);

        try {
            String segmentPath = service.getHLSSegmentPath(id, filename);
            log.info("Segment path from service: {}", segmentPath);

            Path absolutePath = fileStorageService.resolveAbsolute(segmentPath);
            log.info("Absolute path: {}", absolutePath);

            if (!Files.exists(absolutePath)) {
                log.error("Segment file not found at: {}", absolutePath);
                throw new ApiExcepiton("Segment not found");
            }

            long fileSize = Files.size(absolutePath);
            log.info("Segment size: {} bytes", fileSize);

            Resource resource = new FileSystemResource(absolutePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp2t"))
                    .body(resource);

        } catch (Exception e) {
            log.error("=== HLS SEGMENT ERROR ===", e);
            log.error("Video ID: {}, Filename: {}", id, filename);
            throw new ApiExcepiton("Failed to serve segment: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        return ResponseEntity.ok(service.toggleLike(id, authentication));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<VideoPostService.VideoAvailabilityStatus> checkAvailability(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(service.checkVideoAvailability(id, authentication));
    }
}