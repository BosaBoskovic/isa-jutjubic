package com.jutjubic.jutjubic_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "popular_videos")
public class PopularVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pipeline_run_at", nullable = false)
    private LocalDateTime pipelineRunAt;

    @ManyToOne
    @JoinColumn(name = "video_post_id", nullable = false)
    private VideoPost videoPost;

    @Column(name = "popularity_rank", nullable = false)
    private Integer popularityRank; // 1, 2, 3

    @Column(name = "popularity_score", nullable = false)
    private Double popularityScore;

    @Column(name = "views_last_7_days", nullable = false)
    private Long viewsLast7Days;
}