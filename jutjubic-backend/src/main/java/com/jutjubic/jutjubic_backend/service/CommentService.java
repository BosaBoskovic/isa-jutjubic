package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.dto.CommentDto;
import com.jutjubic.jutjubic_backend.exception.ApiExcepiton;
import com.jutjubic.jutjubic_backend.model.Comment;
import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.CommentRepository;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.dto.PagedCommentsDto;
import lombok.Setter;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentService {
    private static final int MAX_COMMENTS_PER_HOUR = 60;
    private static final long WINDOW_SECONDS = 3600;

    private final CommentRepository commentRepository;
    private final VideoPostRepository videoPostRepository;
    private final UserRepository userRepository;
    private final RateLimiterService rateLimiterService;

    public CommentService(
            CommentRepository commentRepository, VideoPostRepository videoPostRepository, UserRepository userRepository, RateLimiterService rateLimiterService
    ) {
        this.commentRepository = commentRepository;
        this.videoPostRepository = videoPostRepository;
        this.userRepository = userRepository;
        this.rateLimiterService = rateLimiterService;
    }

    //create comment
    @Transactional
    @CacheEvict(value = "videoComments", allEntries = true)
    public Comment addComment(Long videoPostId, String text, String principal) {
        if (principal == null) {
            throw new ApiExcepiton("You must be logged in to comment");
        }
        if (text == null || text.isBlank()) {
            throw new ApiExcepiton("Comment text is required");
        }
        User user = userRepository.findByEmail(principal)
                .or(() -> userRepository.findByUsername(principal))
                .orElseThrow(() -> new ApiExcepiton("User not found"));
        VideoPost video = videoPostRepository.findById(videoPostId)
                .orElseThrow(() -> new ApiExcepiton("Video post not found"));

        boolean allowed = rateLimiterService.checkAndIncrement(
                user.getId(),
                MAX_COMMENTS_PER_HOUR,
                WINDOW_SECONDS
        );

        if (!allowed) {
            long currentCount = rateLimiterService.getCurrentCount(user.getId());
            throw new ApiExcepiton(
                    String.format("Comment limit reached (%d/%d comments per hour).",
                            currentCount, MAX_COMMENTS_PER_HOUR)
            );
        }

        Comment comment = new Comment();
        comment.setText(text.trim());
        comment.setAuthor(user);
        comment.setVideoPost(video);
        comment.setCreatedAt(LocalDateTime.now());

        return commentRepository.save(comment);
    }

    //get comments - paginated
    @Transactional(readOnly = true)
    @Cacheable(value = "videoComments", key = "#videoPostId + ':' + #page + ':' + #size")
    public PagedCommentsDto getCommentsCached(Long videoPostId, int page, int size) {

        PageRequest pageable = PageRequest.of(page, size);

        Page<Comment> pageResult =
                commentRepository.findWithAuthor(videoPostId, pageable);

        List<CommentDto> content = pageResult.getContent().stream()
                .map(c -> new CommentDto(
                        c.getId(),
                        c.getText(),
                        c.getAuthor().getUsername(), // SAFE
                        c.getCreatedAt()
                ))
                .toList();

        return new PagedCommentsDto(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );
    }
}
