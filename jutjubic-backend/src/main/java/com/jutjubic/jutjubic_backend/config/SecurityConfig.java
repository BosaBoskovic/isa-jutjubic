package com.jutjubic.jutjubic_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


@Configuration
public class SecurityConfig {

    private final JwtAuthenticationConfig jwtAuthenticationConfig;
    private final UserActivityFilter userActivityFilter;

    public SecurityConfig(JwtAuthenticationConfig jwtAuthenticationConfig,
                          UserActivityFilter userActivityFilter) {
        this.jwtAuthenticationConfig = jwtAuthenticationConfig;
        this.userActivityFilter = userActivityFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/test").permitAll()
                        .requestMatchers("/api/posts/public").permitAll()
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/test").permitAll()
                        .requestMatchers("/api/map/**").permitAll()
                        .requestMatchers("/api/admin/**").permitAll()


                        // Prometheus endpoint
                        .requestMatchers("/actuator/**").permitAll()

                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/watchparty/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/*/hls/**").permitAll()

                        .requestMatchers("/api/popular/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/posts/mine").authenticated()


                        .requestMatchers(HttpMethod.POST, "/api/posts").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/posts/*/like").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                        .requestMatchers("/api/map/**").permitAll()


                        .requestMatchers("/actuator/health/**").permitAll()

                        .requestMatchers("/actuator/info").permitAll()

                        .requestMatchers("/api/demo/**").permitAll()

                        .requestMatchers("/debug/**").permitAll()

                        .requestMatchers("/ws/**").permitAll()


                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationConfig, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(userActivityFilter, JwtAuthenticationConfig.class)  // Dodaj user activity filter
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable());
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}