package com.jutjubic.jutjubic_backend.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ActiveUserMetricsService {

    private final MeterRegistry meterRegistry;

    private final Map<Long, LocalDateTime> activeUsers = new ConcurrentHashMap<>();

    private final Counter userActivityCounter;

    private final AtomicInteger currentActiveUsers = new AtomicInteger(0);

    public ActiveUserMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.userActivityCounter = Counter.builder("app.user.activity.total")
                .description("Total number of user activities")
                .register(meterRegistry);

        Gauge.builder("app.user.active.current", currentActiveUsers, AtomicInteger::get)
                .description("Number of currently active users")
                .register(meterRegistry);

        Gauge.builder("app.user.active.24h", this, service -> service.getActiveUsersLast24Hours())
                .description("Number of active users in last 24 hours")
                .register(meterRegistry);

        log.info("ActiveUserMetricsService initialized with Prometheus metrics");
    }

    public void recordUserActivity(Long userId) {
        if (userId == null) return;

        LocalDateTime now = LocalDateTime.now();
        boolean wasActive = activeUsers.containsKey(userId);

        activeUsers.put(userId, now);
        userActivityCounter.increment();

        if (!wasActive) {
            currentActiveUsers.incrementAndGet();
            log.debug("New active user: {}, total active: {}", userId, currentActiveUsers.get());
        }
    }

    public long getActiveUsersLast24Hours() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        long count = activeUsers.values().stream()
                .filter(lastActivity -> lastActivity.isAfter(cutoff))
                .count();

        return count;
    }

    public void cleanupInactiveUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        activeUsers.entrySet().removeIf(entry -> {
            boolean isInactive = entry.getValue().isBefore(cutoff);
            if (isInactive) {
                currentActiveUsers.decrementAndGet();
            }
            return isInactive;
        });

        log.debug("Cleanup completed. Current active users: {}", currentActiveUsers.get());
    }

    public int getCurrentActiveUsers() {
        return currentActiveUsers.get();
    }
}