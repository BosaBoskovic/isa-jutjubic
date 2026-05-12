package com.jutjubic.jutjubic_backend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter @Setter
    private String token;

    @Getter @Setter
    @OneToOne
    private User user;

    @Getter @Setter
    private LocalDateTime expiryDate;
}
