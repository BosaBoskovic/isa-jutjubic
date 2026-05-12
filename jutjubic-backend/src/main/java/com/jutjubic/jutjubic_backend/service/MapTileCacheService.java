package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.model.TimePeriod;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class MapTileCacheService {

    @CacheEvict(
            value = "mapTiles",
            key = "{#z, #x, #y, #period}"
    )
    public void evictTile(int z, int x, int y, TimePeriod period) {

    }
}

