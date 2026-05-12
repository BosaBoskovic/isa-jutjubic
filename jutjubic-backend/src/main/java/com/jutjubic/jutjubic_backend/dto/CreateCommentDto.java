package com.jutjubic.jutjubic_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentDto {
    @NotBlank
    @Size(max = 1000)
    private String text;
}
