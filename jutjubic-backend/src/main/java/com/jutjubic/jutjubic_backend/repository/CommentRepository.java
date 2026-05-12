package com.jutjubic.jutjubic_backend.repository;

import com.jutjubic.jutjubic_backend.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {


    Page<Comment> findByVideoPost_IdOrderByCreatedAtDesc(Long videoPostId, Pageable pageable);

    @Query("""
    SELECT c FROM Comment c
    JOIN FETCH c.author
    WHERE c.videoPost.id = :videoPostId
    ORDER BY c.createdAt DESC
""")
    Page<Comment> findWithAuthor(
            @Param("videoPostId") Long videoPostId,
            Pageable pageable
    );

    long countByAuthorIdAndCreatedAtAfter(Long authorId, LocalDateTime since);
}
