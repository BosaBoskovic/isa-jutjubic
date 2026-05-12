package com.jutjubic.jutjubic_backend.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@Table(name="video_post",
        indexes = { @Index(name="ix_video_post_created_at", columnList="created_at") })

public class VideoPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column (nullable = false, length=2000)
    private String description;

    @ManyToMany
    @JoinTable(name= "video_post_tags",
               joinColumns = @JoinColumn(name="video_post_id"),
                inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags= new HashSet<>();

    @Column(nullable = false)
    private String thumbnailPath;

    @Column
    private String compressedThumbnailPath;

    @Column(nullable = false)
    private boolean thumbnailCompressed = false;

    @Column(nullable = false)
    private String videoPath;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;


    @Embedded
    private GeoLocation location;

    @ManyToOne(optional = false)
    private User author;

    @Column(nullable = false)
    private long viewsCount = 0;

    // Scheduled video fields
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "is_scheduled")
    private boolean isScheduled = false;

    // HLS streaming fields
    @Column(name = "hls_playlist_path")
    private String hlsPlaylistPath;

    @Column(name = "hls_processed")
    private boolean hlsProcessed = false;

    @Column(name = "video_duration_seconds")
    private Integer videoDurationSeconds;
}
