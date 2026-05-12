package com.jutjubic.jutjubic_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;


@Configuration
public class RabbitDemoConfig {

    public static final String DEMO_QUEUE = "demo.queue";

    @Bean
    public Queue demoQueue() {
        return new Queue(DEMO_QUEUE, true); // durable
    }
}