package com.jutjubic.jutjubic_backend.repository;

import com.jutjubic.jutjubic_backend.model.VideoPostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoPostLikeRepository extends JpaRepository<VideoPostLike, Long> {

    long countByVideoPost_Id(Long postId);

    boolean existsByVideoPost_IdAndUser_Id(Long postId, Long userId);

    void deleteByVideoPost_IdAndUser_Id(Long postId, Long userId);
}

