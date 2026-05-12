package com.jutjubic.jutjubic_backend.config;

import com.jutjubic.jutjubic_backend.model.GeoLocation;
import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.service.HLSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Configuration
@Slf4j
public class DataLoader {

    private static final int VIDEO_COUNT = 5;

    @Bean
    CommandLineRunner loadTestData(
            UserRepository userRepository,
            VideoPostRepository videoPostRepository,
            PasswordEncoder passwordEncoder,
            HLSService hlsService) {

        return args -> {
            User user = userRepository.findByUsername("testuser").orElseGet(() -> {
                User u = new User();
                u.setEmail("test@test.com");
                u.setUsername("testuser");
                u.setPassword(passwordEncoder.encode("test123"));
                u.setEnabled(true);
                u.setFirstName("Test");
                u.setLastName("User");
                return userRepository.save(u);
            });

            // ✅ REGENERATE HLS FOR EXISTING VIDEOS WITHOUT IT
            List<VideoPost> videosWithoutHLS = videoPostRepository.findAll().stream()
                    .filter(v -> !v.isHlsProcessed() || v.getHlsPlaylistPath() == null)
                    .toList();

            if (!videosWithoutHLS.isEmpty()) {
                log.info("🔄 Found {} videos without HLS. Regenerating...", videosWithoutHLS.size());

                for (VideoPost video : videosWithoutHLS) {
                    try {
                        log.info("Generating HLS for video ID: {}", video.getId());

                        String hlsPath = hlsService.convertToHLS(video.getVideoPath());

                        video.setHlsPlaylistPath(hlsPath);
                        video.setHlsProcessed(true);
                        videoPostRepository.save(video);

                        log.info("✅ HLS generated for video ID: {}", video.getId());
                    } catch (Exception e) {
                        log.error("❌ Failed to generate HLS for video ID: {}", video.getId(), e);
                        // Mark as failed but don't crash
                        video.setHlsProcessed(false);
                        videoPostRepository.save(video);
                    }
                }

                log.info("✅ HLS regeneration completed!");
            }

            // Check if we need to create new test videos
            if (videoPostRepository.count() >= VIDEO_COUNT) {
                System.out.println("Test videi vec postoje");
                return;
            }

            // ✅ CREATE NEW TEST VIDEOS WITH HLS
            Random random = new Random();

            for (int i = 0; i < VIDEO_COUNT; i++) {
                VideoPost video = new VideoPost();
                video.setTitle("Test video " + i);
                video.setDescription("Test video");
                video.setThumbnailPath("thumbnails/test-thumb.jpeg");
                video.setVideoPath("videos/test-video.mp4");
                video.setCreatedAt(randomDate(random));
                video.setViewsCount(random.nextInt(10_000));
                video.setAuthor(user);

                GeoLocation location = new GeoLocation();
                location.setLatitude(randomLatEurope(random));
                location.setLongitude(randomLngEurope(random));
                video.setLocation(location);

                //  GENERATE HLS FOR NEW TEST VIDEO
                try {
                    log.info("Creating test video {} with HLS...", i);
                    String hlsPath = hlsService.convertToHLS(video.getVideoPath());
                    video.setHlsPlaylistPath(hlsPath);
                    video.setHlsProcessed(true);
                    log.info("HLS created for test video {}", i);
                } catch (Exception e) {
                    log.error("Failed to create HLS for test video {}", i, e);
                    video.setHlsProcessed(false);
                }

                videoPostRepository.save(video);
            }

            System.out.println("Ubaceno " + VIDEO_COUNT + " testnih snimaka sa HLS!");
        };
    }

    private double randomLatEurope(Random r) {
        return 35 + (70 - 35) * r.nextDouble();
    }

    private double randomLngEurope(Random r) {
        return -10 + (40 + 10) * r.nextDouble();
    }

    private LocalDateTime randomDate(Random r) {
        return LocalDateTime.now().minusDays(r.nextInt(365));
    }
}