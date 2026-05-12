package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.UploadEvent;
import com.jutjubic.jutjubic_backend.messaging.UploadEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/debug/mq")
@RequiredArgsConstructor
public class MqBenchmarkController {

    private final UploadEventPublisher publisher;

    // u browseru: http://localhost:8080/debug/mq/benchmark?count=50
    @GetMapping("/benchmark")
    public String benchmark(@RequestParam(defaultValue = "50") int count) {

        for (int i = 1; i <= count; i++) {
            UploadEvent ev = UploadEvent.builder()
                    .videoId((long) i)
                    .title("Test video " + i)
                    .authorEmail("test@example.com")
                    .sizeBytes(123456L + i)
                    .createdAt(LocalDateTime.now().toString())
                    .build();

            publisher.publishBoth(ev);
        }

        return "Sent " + count + " JSON + " + count + " PROTO messages.";
    }
}
