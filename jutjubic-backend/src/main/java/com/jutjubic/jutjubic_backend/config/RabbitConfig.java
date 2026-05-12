package com.jutjubic.jutjubic_backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String QUEUE = "video.transcode.queue";


    public static final String UPLOAD_EXCHANGE = "video.upload.exchange";

    public static final String UPLOAD_JSON_QUEUE = "video.upload.json.q";
    public static final String UPLOAD_PROTO_QUEUE = "video.upload.proto.q";

    public static final String UPLOAD_JSON_KEY = "video.upload.json";
    public static final String UPLOAD_PROTO_KEY = "video.upload.proto";



    @Bean
    public Queue queue() {
        return QueueBuilder
                .durable(QUEUE)
                .build();
    }


    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }


    @Bean
    public TopicExchange uploadExchange() {
        return new TopicExchange(UPLOAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue uploadJsonQueue() {
        return QueueBuilder.durable(UPLOAD_JSON_QUEUE).build();
    }

    @Bean
    public Queue uploadProtoQueue() {
        return QueueBuilder.durable(UPLOAD_PROTO_QUEUE).build();
    }

    @Bean
    public Binding uploadJsonBinding(TopicExchange uploadExchange, Queue uploadJsonQueue) {
        return BindingBuilder.bind(uploadJsonQueue).to(uploadExchange).with(UPLOAD_JSON_KEY);
    }

    @Bean
    public Binding uploadProtoBinding(TopicExchange uploadExchange, Queue uploadProtoQueue) {
        return BindingBuilder.bind(uploadProtoQueue).to(uploadExchange).with(UPLOAD_PROTO_KEY);
    }



}