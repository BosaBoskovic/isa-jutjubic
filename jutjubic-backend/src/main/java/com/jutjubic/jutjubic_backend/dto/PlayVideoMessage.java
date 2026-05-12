package com.jutjubic.jutjubic_backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayVideoMessage {
    private Long videoId;
    private String roomId;
}
