package com.jutjubic.jutjubic_backend.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter @Setter
public class CreateVideoPostRequestDto {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 2000)
    private String description;

    @NotEmpty
    private Set<String> tags;

    private Double latitude;
    private Double longitude;

    // Scheduled publishing
    private LocalDateTime scheduledAt;
}
