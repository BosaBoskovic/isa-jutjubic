package com.jutjubic.jutjubic_backend.config;


import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableScheduling
public class MetricsConfig {

    private final AtomicInteger activeUsers24h = new AtomicInteger(0);
    private final AtomicInteger currentActiveUsers = new AtomicInteger(0);

    @Bean
    public AtomicInteger activeUsers24hCounter(MeterRegistry registry) {
        Gauge.builder("app_user_active_24h", activeUsers24h, AtomicInteger::get)
                .description("Number of active users in last 24 hours")
                .tag("application", "jutjubic-backend")
                .register(registry);
        return activeUsers24h;
    }

    @Bean
    public AtomicInteger currentActiveUsersCounter(MeterRegistry registry) {
        Gauge.builder("app_user_active_current", currentActiveUsers, AtomicInteger::get)
                .description("Number of currently active users")
                .tag("application", "jutjubic-backend")
                .register(registry);
        return currentActiveUsers;
    }
}
