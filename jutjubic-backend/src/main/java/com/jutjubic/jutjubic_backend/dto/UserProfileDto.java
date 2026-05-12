package com.jutjubic.jutjubic_backend.dto;

import com.jutjubic.jutjubic_backend.dto.VideoPostDto;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class UserProfileDto {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;

    private List<VideoPostDto> videos;
}
