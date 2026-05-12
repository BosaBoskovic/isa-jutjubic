package com.jutjubic.jutjubic_backend.config;

import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.service.ActiveUserMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActivityFilter extends OncePerRequestFilter {

    private final ActiveUserMetricsService activeUserMetricsService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Prati aktivnost samo za autentifikovane korisnike
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            try {
                // Pronađi korisnika po email-u ili username-u
                User user = userRepository.findByEmail(auth.getName())
                        .or(() -> userRepository.findByUsername(auth.getName()))
                        .orElse(null);

                if (user != null) {
                    activeUserMetricsService.recordUserActivity(user.getId());
                }
            } catch (Exception e) {
                log.error("Error recording user activity", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}