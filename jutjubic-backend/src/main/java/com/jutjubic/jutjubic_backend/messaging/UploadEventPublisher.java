package com.jutjubic.jutjubic_backend.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.jutjubic.jutjubic_backend.config.RabbitConfig;
import com.jutjubic.jutjubic_backend.dto.UploadEvent;
import com.jutjubic.jutjubic_backend.messaging.proto.UploadEventProto;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UploadEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final BenchmarkStats stats;

    public void publishBoth(UploadEvent event) {
        publishJson(event);
        publishProto(event);
    }

    public void publishJson(UploadEvent event) {
        try {
            long t0 = System.nanoTime();
            byte[] body = objectMapper.writeValueAsBytes(event);
            long t1 = System.nanoTime();
            stats.addJsonSerialize(t1 - t0, body.length);

            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setContentEncoding("utf-8");
            // opciono: props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

            rabbitTemplate.send(
                    RabbitConfig.UPLOAD_EXCHANGE,
                    RabbitConfig.UPLOAD_JSON_KEY,
                    new Message(body, props)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void publishProto(UploadEvent event) {
        try {
            UploadEventProto.UploadEvent msg = UploadEventProto.UploadEvent.newBuilder()
                    .setVideoId(event.getVideoId() == null ? 0L : event.getVideoId())
                    .setTitle(event.getTitle() == null ? "" : event.getTitle())
                    .setAuthorEmail(event.getAuthorEmail() == null ? "" : event.getAuthorEmail())
                    .setSizeBytes(event.getSizeBytes() == null ? 0L : event.getSizeBytes())
                    .setCreatedAt(event.getCreatedAt() == null ? "" : event.getCreatedAt())
                    .build();

            long t0 = System.nanoTime();
            byte[] body = msg.toByteArray();
            long t1 = System.nanoTime();
            stats.addProtoSerialize(t1 - t0, body.length);

            MessageProperties props = new MessageProperties();
            props.setContentType("application/x-protobuf");

            rabbitTemplate.send(
                    RabbitConfig.UPLOAD_EXCHANGE,
                    RabbitConfig.UPLOAD_PROTO_KEY,
                    new Message(body, props)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}