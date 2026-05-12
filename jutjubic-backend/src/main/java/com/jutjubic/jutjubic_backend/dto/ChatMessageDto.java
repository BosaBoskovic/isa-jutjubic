package com.jutjubic.jutjubic_backend.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ChatMessageDto {
    private Long videoId;
    private String message;

    // server popunjava:
    private String sender;
    private Instant timestamp;
}
