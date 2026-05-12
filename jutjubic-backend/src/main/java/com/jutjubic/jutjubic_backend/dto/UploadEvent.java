package com.jutjubic.jutjubic_backend.dto;

import lombok.*;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadEvent {
    private Long videoId;
    private String title;
    private String authorEmail;
    private Long sizeBytes;
    private String createdAt; // ISO string
}