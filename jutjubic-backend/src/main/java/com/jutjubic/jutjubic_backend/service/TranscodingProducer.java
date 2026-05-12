package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.config.RabbitConfig;
import com.jutjubic.jutjubic_backend.dto.TranscodingRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class TranscodingProducer {

    private final RabbitTemplate rabbitTemplate;

    public TranscodingProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendForTranscoding(TranscodingRequest request) {
        System.out.println("📨 Producer: Sending message to queue...");
        System.out.println("   Video ID: " + request.getVideoId());
        System.out.println("   Video Path: " + request.getVideoPath());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitConfig.QUEUE,
                    request
            );
            System.out.println("✅ Producer: Message sent successfully!");
        } catch (Exception e) {
            System.err.println("❌ Producer: Failed to send message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}