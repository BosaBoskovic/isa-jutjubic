package com.jutjubic.jutjubic_backend.controller;


import com.jutjubic.jutjubic_backend.config.RabbitDemoConfig;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
public class DemoMqController {

    private final RabbitTemplate rabbitTemplate;

    public DemoMqController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @GetMapping("/mq/publish")

    public ResponseEntity<?> publish(@RequestParam(defaultValue = "hello") String msg) {
        try {
            String payload = msg + " @ " + Instant.now();
            rabbitTemplate.convertAndSend(RabbitDemoConfig.DEMO_QUEUE, payload);

            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "queue", RabbitDemoConfig.DEMO_QUEUE,
                    "message", payload
            ));
        } catch (AmqpException ex) {
            // MQ down / connection issue
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "status", "MQ_DOWN",
                    "error", ex.getClass().getSimpleName(),
                    "message", "RabbitMQ nije dostupan (ali aplikacija radi)."
            ));
        }
    }
}
