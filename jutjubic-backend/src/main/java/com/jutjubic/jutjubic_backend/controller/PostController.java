package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.model.Post;
import com.jutjubic.jutjubic_backend.service.PostService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/public")
    public List<Post> getAllPublicPosts() {
        return postService.getAllPublicPosts();
    }
}
