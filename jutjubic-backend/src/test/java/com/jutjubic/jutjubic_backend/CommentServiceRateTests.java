package com.jutjubic.jutjubic_backend;

import com.jutjubic.jutjubic_backend.exception.ApiExcepiton;
import com.jutjubic.jutjubic_backend.model.Comment;
import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.CommentRepository;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.service.CommentService;
import com.jutjubic.jutjubic_backend.service.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional  // 👈 Automatski rollback posle svakog testa
class CommentServiceRateLimitTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VideoPostRepository videoPostRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private VideoPost testVideo;

    @BeforeEach
    void setup() {
        // Obriši postojeće test komentare
        commentRepository.deleteAll();

        // Koristi postojećeg korisnika ili kreiraj novog
        String testEmail = "ratelimit-test@example.com";
        String testUsername = "ratelimit-testuser-" + System.currentTimeMillis(); // Unique username

        testUser = userRepository.findByEmail(testEmail).orElseGet(() -> {
            User u = new User();
            u.setEmail(testEmail);
            u.setUsername(testUsername);
            u.setPassword(passwordEncoder.encode("test123"));
            u.setEnabled(true);
            u.setFirstName("Test");
            u.setLastName("User");
            return userRepository.save(u);
        });

        // Resetuj rate limiter za ovog korisnika
        rateLimiterService.reset(testUser.getId());

        // Koristi prvi dostupan video
        testVideo = videoPostRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No video found. Please create at least one video first."));

        System.out.println("✅ Test setup complete. User ID: " + testUser.getId() + ", Video ID: " + testVideo.getId());
    }

    @Test
    void testRateLimiter_shouldAllowUpTo60Comments() {
        System.out.println("🧪 Testing: Should allow up to 60 comments");

        for (int i = 1; i <= 60; i++) {
            Comment comment = commentService.addComment(
                    testVideo.getId(),
                    "Test comment #" + i,
                    testUser.getEmail()
            );

            assertNotNull(comment.getId());

            if (i % 10 == 0) {
                System.out.println("✅ Created " + i + "/60 comments");
            }
        }

        System.out.println("✅ SUCCESS: All 60 comments created");
    }

    @Test
    void testRateLimiter_shouldBlock61stComment() {
        System.out.println("🧪 Testing: Should block 61st comment");

        // Dodaj 60 komentara
        for (int i = 1; i <= 60; i++) {
            commentService.addComment(
                    testVideo.getId(),
                    "Test comment #" + i,
                    testUser.getEmail()
            );
        }

        System.out.println("✅ Created 60 comments successfully");
        System.out.println("🚫 Attempting to create 61st comment (should FAIL)...");

        // 61. komentar bi trebalo da FAIL-uje
        ApiExcepiton exception = assertThrows(ApiExcepiton.class, () -> {
            commentService.addComment(
                    testVideo.getId(),
                    "This should FAIL - comment #61",
                    testUser.getEmail()
            );
        });

        assertTrue(exception.getMessage().contains("Comment limit reached"));
        System.out.println("✅ SUCCESS: 61st comment correctly BLOCKED");
        System.out.println("   Error message: " + exception.getMessage());
    }
}