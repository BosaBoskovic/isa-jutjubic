package com.jutjubic.jutjubic_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class RegisterRequestDto {

    @Email
    @NotBlank
    @Getter
    @Setter
    private String email;

    @NotBlank
    @Getter
    @Setter
    private String username;

    @Size(min=8, message = "Password must be at least 8 characters")
    @NotBlank(message = "Password is required")
    @Getter
    @Setter
    private String password;

    @NotBlank(message = "Confirm password is required")
    @Getter
    @Setter
    private String confirmPassword;

    @NotBlank
    @Getter
    @Setter
    private String firstName;

    @NotBlank
    @Getter
    @Setter
    private String lastName;

    @NotBlank
    @Getter
    @Setter
    private String address;
}
