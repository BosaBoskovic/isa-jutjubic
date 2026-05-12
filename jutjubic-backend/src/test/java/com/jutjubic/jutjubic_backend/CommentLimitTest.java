package com.jutjubic.jutjubic_backend;

import com.jutjubic.jutjubic_backend.exception.ApiExcepiton;
import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.service.CommentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class CommentLimitTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Test
    void shouldLimitCommentsTo60PerHourWithoutRateLimiter() {

        // --- create user ---
        User user = new User();
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        user.setEmail("user" + suffix + "@test.com");
        user.setUsername("user" + suffix);
        user.setPassword("pass");
        user.setEnabled(true);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAddress("Address");
        user = userRepository.save(user);

        // --- create video post ---
        VideoPost video = new VideoPost();
        video.setTitle("Test video");
        video.setDescription("Description");
        video.setThumbnailPath("thumb.jpg");
        video.setVideoPath("video.mp4");
        video.setCreatedAt(LocalDateTime.now());
        video.setAuthor(user);
        video.setViewsCount(0);
        video = videoPostRepository.save(video);

        Long videoId = video.getId();
        String principal = user.getUsername();

        // --- simulate large number of comments ---
        for (int i = 1; i <= 60; i++) {
            int index = i;
            assertDoesNotThrow(() ->
                    commentService.addComment(
                            videoId,
                            "Comment " + index,
                            principal
                    )
            );
        }

        // --- 61st comment must fail ---
        assertThrows(ApiExcepiton.class, () ->
                commentService.addComment(
                        videoId,
                        "This comment should be rejected",
                        principal
                )
        );
    }
}