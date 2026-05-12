package com.jutjubic.jutjubic_backend.config;

import com.jutjubic.jutjubic_backend.service.CustomUserDetailsService;
import com.jutjubic.jutjubic_backend.service.JwtUtilService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServletServerHttpRequest;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtilService jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return true; // fallback
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            // bez tokena -> NE dozvoljavamo chat (zahtev kaže “među korisnicima”)
            return false;
        }

        try {
            String email = jwtUtil.extractEmail(token);
            var userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtUtil.validateToken(token, userDetails)) {
                // upiši identitet u attributes da ga posle preuzme HandshakeHandler
                attributes.put("userEmail", email);
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
