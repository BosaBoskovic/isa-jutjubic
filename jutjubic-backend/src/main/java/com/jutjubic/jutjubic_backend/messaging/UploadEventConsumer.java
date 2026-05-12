package com.jutjubic.jutjubic_backend.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jutjubic.jutjubic_backend.config.RabbitConfig;
import com.jutjubic.jutjubic_backend.dto.UploadEvent;
import com.jutjubic.jutjubic_backend.messaging.proto.UploadEventProto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UploadEventConsumer {

    private final ObjectMapper objectMapper;
    private final BenchmarkStats stats;

   // @RabbitListener(queues = RabbitConfig.UPLOAD_JSON_QUEUE)
   @RabbitListener(queues = RabbitConfig.UPLOAD_JSON_QUEUE, autoStartup = "${mq.upload.listeners.enabled:true}")
    public void onJson(byte[] payload) {
        try {
            long t0 = System.nanoTime();
            UploadEvent ev = objectMapper.readValue(payload, UploadEvent.class);
            long t1 = System.nanoTime();

            stats.addJsonDeserialize(t1 - t0, payload.length);
            System.out.println("JSON received: " + ev.getTitle());
        } catch (Exception e) {
            System.err.println("Failed to deserialize JSON UploadEvent: " + e.getMessage());
        }
    }

   // @RabbitListener(queues = RabbitConfig.UPLOAD_PROTO_QUEUE)
   @RabbitListener(queues = RabbitConfig.UPLOAD_PROTO_QUEUE, autoStartup = "${mq.upload.listeners.enabled:true}")
    public void onProto(byte[] payload) {
        try {
            long t0 = System.nanoTime();
            var ev = UploadEventProto.UploadEvent.parseFrom(payload);
            long t1 = System.nanoTime();

            stats.addProtoDeserialize(t1 - t0, payload.length);
            System.out.println("PROTO received: " + ev.getTitle());
        } catch (Exception e) {
            System.err.println("Failed to deserialize PROTO UploadEvent: " + e.getMessage());
        }
    }
}