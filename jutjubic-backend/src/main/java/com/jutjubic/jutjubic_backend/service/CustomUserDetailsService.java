package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
        throws UsernameNotFoundException{

        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User does not exist."));
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.isEnabled())
                .authorities("ROLE_USER")
                .build();
    }
}
