package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.UserProfileDto;
import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{username}/public")
    public UserProfileDto getUserProfile(@PathVariable String username) {
        return userService.getPublicUserByUsername(username); // DTO metoda
    }

}
