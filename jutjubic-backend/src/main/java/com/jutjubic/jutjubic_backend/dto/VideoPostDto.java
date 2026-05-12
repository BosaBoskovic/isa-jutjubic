package com.jutjubic.jutjubic_backend.dto;

import com.jutjubic.jutjubic_backend.model.Tag;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
public class VideoPostDto {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime createdAt;
    private String authorName;
    private String authorUsername;
    private Set<String> tags;
    private Double latitude;
    private Double longitude;

    private long likesCount;
    private Boolean likedByMe;

    private long viewsCount;

    // Scheduled fields
    private boolean isScheduled;
    private LocalDateTime scheduledAt;
    private boolean isLive;  // True ako je video trenutno live
    private Integer videoDurationSeconds;
    private Long currentStreamOffsetSeconds;  // Za synchronized streaming


    public static VideoPostDto from(VideoPost p) {
        VideoPostDto dto = new VideoPostDto();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        dto.setDescription(p.getDescription());
        dto.setCreatedAt(p.getCreatedAt());
        dto.setAuthorName(p.getAuthor().getUsername());
        dto.setAuthorUsername(p.getAuthor().getUsername());

        if (p.getTags() != null) {
            dto.setTags(
                    p.getTags().stream()
                            .map(Tag::getName)
                            .collect(Collectors.toSet())
            );
        }

        if (p.getLocation() != null) {
            dto.setLatitude(p.getLocation().getLatitude());
            dto.setLongitude(p.getLocation().getLongitude());
        }

        dto.setViewsCount(p.getViewsCount());

        // Scheduled fields
        dto.setScheduled(p.isScheduled());
        dto.setScheduledAt(p.getScheduledAt());
        dto.setVideoDurationSeconds(p.getVideoDurationSeconds());

        // Calculate if video is currently live
        if (p.isScheduled() && p.getScheduledAt() != null) {
            LocalDateTime now = LocalDateTime.now();
            boolean isLive = !now.isBefore(p.getScheduledAt());
            dto.setLive(isLive);

            // Calculate current offset for synchronized streaming
            if (isLive && p.getVideoDurationSeconds() != null) {
                long secondsSinceStart = java.time.Duration.between(p.getScheduledAt(), now).getSeconds();
                // Loop the video if it's longer than duration
                long offset = secondsSinceStart % p.getVideoDurationSeconds();
                dto.setCurrentStreamOffsetSeconds(offset);
            }
        }

        return dto;
    }
}

