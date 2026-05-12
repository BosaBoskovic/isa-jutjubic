package com.jutjubic.jutjubic_backend.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MapTileNightJob {

    @CacheEvict(value = {"mapTiles", "mapClusters"}, allEntries = true)
    @Scheduled(cron = "0 0 3 * * *")
    public void nightlyEvictToForceRecompute() {
    }
}

