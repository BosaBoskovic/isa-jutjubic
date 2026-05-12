package com.jutjubic.jutjubic_backend.service;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.dto.UserProfileDto;
import com.jutjubic.jutjubic_backend.model.User;
import com.jutjubic.jutjubic_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.jutjubic.jutjubic_backend.dto.UserProfileDto;
import com.jutjubic.jutjubic_backend.dto.VideoPostDto;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import java.util.stream.Collectors;
import java.util.List;


@Service
public class UserService {

    private final UserRepository userRepository;
    private final VideoPostRepository videoPostRepository;

    public UserService(UserRepository userRepository, VideoPostRepository videoPostRepository) {
        this.userRepository = userRepository;
        this.videoPostRepository = videoPostRepository;
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    public UserProfileDto getPublicUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());

        List<VideoPostDto> videos = videoPostRepository.findAllByAuthorUsername(username)
                .stream()
                .map(VideoPostDto::from)
                .collect(Collectors.toList());

        dto.setVideos(videos);

        return dto;
    }
}