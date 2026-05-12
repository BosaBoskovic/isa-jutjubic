package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.PopularVideoDto;
import com.jutjubic.jutjubic_backend.service.PopularVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/popular")
@RequiredArgsConstructor
public class PopularVideoController {

    private final PopularVideoService popularVideoService;

    @GetMapping("/top3")
    public ResponseEntity<List<PopularVideoDto>> getTop3PopularVideos() {
        List<PopularVideoDto> top3 = popularVideoService.getTop3PopularVideos();
        return ResponseEntity.ok(top3);
    }
}