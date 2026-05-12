package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.exception.ApiExcepiton;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.model.VideoView;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.repository.VideoViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class ViewCounterService {

    private final JdbcTemplate jdbcTemplate;
    private final VideoViewRepository videoViewRepository;
    private final VideoPostRepository videoPostRepository;


    @Transactional
    public long incrementAndGet(Long postId) {
        try {
            Long newValue = jdbcTemplate.queryForObject(
                    "UPDATE video_post SET views_count = views_count + 1 WHERE id = ? RETURNING views_count",
                    Long.class,
                    postId
            );

            if (newValue == null) {
                throw new ApiExcepiton("Post not found");
            }

            logView(postId);

            return newValue;

        } catch (EmptyResultDataAccessException e) {
            throw new ApiExcepiton("Post not found");
        }
    }

    private void logView(Long postId) {
        try {
            VideoPost videoPost = videoPostRepository.findById(postId)
                    .orElseThrow(() -> new ApiExcepiton("Post not found"));

            VideoView view = new VideoView();
            view.setVideoPost(videoPost);
            view.setViewedAt(LocalDateTime.now());

            videoViewRepository.save(view);

        } catch (Exception e) {
            System.err.println("Failed to log view for post " + postId + ": " + e.getMessage());
        }
    }
}
