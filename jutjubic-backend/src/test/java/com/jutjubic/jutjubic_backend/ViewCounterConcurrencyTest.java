package com.jutjubic.jutjubic_backend;

import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.service.ViewCounterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
public class ViewCounterConcurrencyTest {

    @Autowired private ViewCounterService viewCounterService;
    @Autowired private VideoPostRepository postRepo;
    @Autowired private UserRepository userRepo;

    @Test
    void shouldIncrementViewsCorrectlyUnderConcurrency() throws Exception {
        User u = new User();

        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        u.setEmail("u" + suffix + "@test.com");
        u.setUsername("u" + suffix);
        u.setPassword("x");
        u.setEnabled(true);
        u.setFirstName("A");
        u.setLastName("B");
        u.setAddress("C");
        u = userRepo.save(u);

        VideoPost p = new VideoPost();
        p.setTitle("t");
        p.setDescription("d");
        p.setThumbnailPath("thumbnails/x.jpg");
        p.setVideoPath("videos/x.mp4");
        p.setCreatedAt(LocalDateTime.now());
        p.setAuthor(u);
        p.setViewsCount(0);

        VideoPost saved = postRepo.save(p);
        final Long postId = saved.getId();

        int threads = 50;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    viewCounterService.incrementAndGet(postId);
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        pool.shutdown();

        VideoPost after = postRepo.findById(postId).orElseThrow();
        assertEquals(threads, after.getViewsCount());
    }

}
