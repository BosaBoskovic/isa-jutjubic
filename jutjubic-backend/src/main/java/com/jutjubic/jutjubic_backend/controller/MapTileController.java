package com.jutjubic.jutjubic_backend.controller;

import com.jutjubic.jutjubic_backend.dto.MapTileResponseDto;
import com.jutjubic.jutjubic_backend.dto.MapVideoDto;
import com.jutjubic.jutjubic_backend.model.TimePeriod;
import com.jutjubic.jutjubic_backend.service.MapTileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/map")
public class MapTileController {

    private final MapTileService tileService;

    public MapTileController(MapTileService tileService) {
        this.tileService = tileService;
    }

    @GetMapping("/tile")
    public MapTileResponseDto tile(@RequestParam int z, @RequestParam int x, @RequestParam int y,
                                   @RequestParam(defaultValue="ALL") TimePeriod period) {
        return tileService.getTile(z, x, y, period);
    }

}

