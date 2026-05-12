package com.jutjubic.jutjubic_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "video_views",
        indexes = {
                @Index(name = "ix_video_views_post_viewed", columnList = "video_post_id,viewed_at"),
                @Index(name = "ix_video_views_viewed_at", columnList = "viewed_at")
        })
public class VideoView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "video_post_id", nullable = false)
    private VideoPost videoPost;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "user_ip")
    private String userIp; // opciono, za analitiku
}