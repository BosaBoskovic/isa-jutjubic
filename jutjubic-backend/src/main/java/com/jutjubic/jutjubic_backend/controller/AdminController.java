package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.service.ThumbnailCompressionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ThumbnailCompressionService compressionService;

    public AdminController(ThumbnailCompressionService compressionService) {
        this.compressionService = compressionService;
    }

    @GetMapping("/compress-thumbnails")
    public ResponseEntity<String> compressThumbnailsGet() {
        compressionService.runManually();
        return ResponseEntity.ok("Compression job started! Check console.");
    }

    @PostMapping("/compress-thumbnails")
    public ResponseEntity<String> compressThumbnails() {
        compressionService.runManually();
        return ResponseEntity.ok("Compression job started! Check console.");
    }
}