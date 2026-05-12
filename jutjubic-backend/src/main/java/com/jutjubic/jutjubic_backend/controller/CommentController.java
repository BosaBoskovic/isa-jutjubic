package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.CommentDto;
import com.jutjubic.jutjubic_backend.dto.CreateCommentDto;
import com.jutjubic.jutjubic_backend.dto.PagedCommentsDto;
import com.jutjubic.jutjubic_backend.model.Comment;
import com.jutjubic.jutjubic_backend.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentDto request,
            Authentication authentication
    ) {
        String principal = authentication != null ? authentication.getName() : null;

        Comment comment = commentService.addComment(
                postId,
                request.getText(),
                principal
        );

        return ResponseEntity.ok(CommentDto.from(comment));
    }

    @GetMapping
    public ResponseEntity<PagedCommentsDto> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedCommentsDto result = commentService.getCommentsCached(postId, page, size);
        return ResponseEntity.ok(result);
    }
}

