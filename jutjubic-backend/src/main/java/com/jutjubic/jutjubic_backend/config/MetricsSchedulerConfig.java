package com.jutjubic.jutjubic_backend.config;

import com.jutjubic.jutjubic_backend.service.ActiveUserMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class MetricsSchedulerConfig {

    private final ActiveUserMetricsService activeUserMetricsService;

    @Scheduled(fixedRate = 900000) // 15 minuta
    public void cleanupInactiveUsers() {
        log.debug("Running scheduled cleanup of inactive users");
        activeUserMetricsService.cleanupInactiveUsers();
    }
}