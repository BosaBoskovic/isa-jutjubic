package com.jutjubic.jutjubic_backend.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class MapClusterCacheService {

    @CacheEvict(value = "mapClusters", allEntries = true)
    public void evictAll() {}
}

