package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.LoginRequestDto;
import com.jutjubic.jutjubic_backend.dto.LoginResponseDto;
import com.jutjubic.jutjubic_backend.dto.RegisterRequestDto;
import com.jutjubic.jutjubic_backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequestDto request) {

        authService.register(request);
        return ResponseEntity.ok("Successful registration!");
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyAccount(@RequestParam("token") String token){
        authService.verifyAccount(token);
        return ResponseEntity.ok("Account verified succesfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIP(httpRequest);
        LoginResponseDto response = authService.login(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
