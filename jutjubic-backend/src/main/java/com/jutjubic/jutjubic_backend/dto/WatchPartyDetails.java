package com.jutjubic.jutjubic_backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WatchPartyDetails {
    private Long id;
    private String name;
    private String inviteCode;
    private String creatorUsername;
    private LocalDateTime createdAt;
    private boolean active;
    private List<String> memberUsernames;  // Lista članova
}