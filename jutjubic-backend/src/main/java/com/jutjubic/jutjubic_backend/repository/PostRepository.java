package com.jutjubic.jutjubic_backend.repository;

import com.jutjubic.jutjubic_backend.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long>{
    List<Post>findAllByOrderByCreatedAtDesc();
}

