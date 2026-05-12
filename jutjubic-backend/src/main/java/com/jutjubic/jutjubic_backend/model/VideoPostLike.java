package com.jutjubic.jutjubic_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "video_post_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_post_id"})
)
@Getter @Setter
public class VideoPostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "video_post_id")
    private VideoPost videoPost;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

