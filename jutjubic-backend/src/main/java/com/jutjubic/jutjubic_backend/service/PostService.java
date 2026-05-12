package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.model.Post;
import com.jutjubic.jutjubic_backend.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {
    private final PostRepository postRepository;

    public PostService(PostRepository postRepository){
        this.postRepository = postRepository;
    }

    public List<Post> getAllPublicPosts(){
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

}