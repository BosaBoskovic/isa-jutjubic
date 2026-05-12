package com.jutjubic.jutjubic_backend.model;

import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
public class Comment {
    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(optional = false, fetch = FetchType.LAZY) //LAZY da ne vuce usera i video bez potrebe
    private User author;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private VideoPost videoPost;
}
