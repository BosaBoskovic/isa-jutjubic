package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.MapVideoDto;
import com.jutjubic.jutjubic_backend.model.TimePeriod;
import com.jutjubic.jutjubic_backend.service.MapVideoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class MapVideoController {

    private final MapVideoService mapService;

    public MapVideoController(MapVideoService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/videos")
    public List<MapVideoDto> getMapVideos(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLng,
            @RequestParam double maxLng,
            @RequestParam int zoom,
            @RequestParam(defaultValue = "ALL") TimePeriod period
    ) {
        return mapService.getMapVideos(minLat, maxLat, minLng, maxLng, zoom, period);
    }
}
