package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.dto.LoginRequestDto;
import com.jutjubic.jutjubic_backend.dto.LoginResponseDto;
import com.jutjubic.jutjubic_backend.dto.RegisterRequestDto;
import com.jutjubic.jutjubic_backend.exception.ApiExcepiton;
import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.model.VerificationToken;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.repository.VerificationTokenRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class  AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final AuthenticationManager authenticationManager;
    private final JwtUtilService jwtUtilService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, VerificationTokenRepository tokenRepository, JavaMailSender mailSender, AuthenticationManager authenticationManager, JwtUtilService jwtUtilService, LoginAttemptService loginAttemptService){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.authenticationManager = authenticationManager;
        this.jwtUtilService = jwtUtilService;
        this.loginAttemptService = loginAttemptService;
    }


    public void register(RegisterRequestDto requestDto){

        if(!requestDto.getPassword().equals(requestDto.getConfirmPassword())){
            throw new ApiExcepiton("Passwords do not match!");
        }

        if(userRepository.existsByEmail(requestDto.getEmail())){
            throw  new ApiExcepiton("Email already exists!");
        }

        if(userRepository.existsByUsername(requestDto.getUsername())){
            throw  new ApiExcepiton("Username already exists!");
        }

        User user = new User();
        user.setEmail((requestDto.getEmail()));
        user.setUsername(requestDto.getUsername());
        user.setPassword(passwordEncoder.encode(requestDto.getPassword()));
        user.setFirstName(requestDto.getFirstName());
        user.setLastName(requestDto.getLastName());
        user.setAddress(requestDto.getAddress());
        user.setEnabled(false);

        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setToken(token);
        verificationToken.setUser(user);
        verificationToken.setExpiryDate(LocalDateTime.now().plusHours(24));
        tokenRepository.save(verificationToken);

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(user.getEmail());
        mailMessage.setSubject("Activate your account");
        mailMessage.setText("Click the link to activate your account: " + "http://localhost:5173/verify?token=" + token);
        mailSender.send(mailMessage);
    }

    public void verifyAccount(String token) {
        VerificationToken verificationToken = tokenRepository.findByToken(token).orElseThrow(() -> new ApiExcepiton("Invalid token"));

        if(verificationToken.getExpiryDate().isBefore(LocalDateTime.now())){
            throw  new ApiExcepiton("Token expired");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepository.save(user);
    }

    public LoginResponseDto login(LoginRequestDto requestDto, String ipAddress) {

        if (loginAttemptService.isBlocked(ipAddress)) {
            throw new ApiExcepiton("Too many failed login attempts. Please try again in 1 minute.");
        }

        try {
            authenticationManager.authenticate(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            requestDto.getEmail(),
                            requestDto.getPassword()
                    )
            );

            User user = userRepository.findByEmail(requestDto.getEmail())
                    .orElseThrow(() -> new ApiExcepiton("User not found"));

            if (!user.isEnabled()) {
                throw new ApiExcepiton("Account not activated. Please check your email.");
            }

            loginAttemptService.loginSucceeded(ipAddress);

            String token = jwtUtilService.generateToken(user.getEmail());
            return new LoginResponseDto(token, user.getEmail(), user.getUsername());

        } catch (org.springframework.security.core.AuthenticationException e) {
            loginAttemptService.loginFailed(ipAddress);
            throw new ApiExcepiton("Invalid email or password");
        }
    }
}
