package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.model.TimePeriod;
import com.jutjubic.jutjubic_backend.service.map.TileUtil;
import org.springframework.stereotype.Service;

@Service
public class MapTileInvalidationService {

    private final MapTileCacheService cache;
    private final MapClusterCacheService clusterCache;

    public MapTileInvalidationService(MapTileCacheService cache, MapClusterCacheService clusterCache) {
        this.cache = cache;
        this.clusterCache = clusterCache;
    }

    public void invalidateForNewVideo(double lat, double lon) {
        for (int z = 0; z <= 20; z++) {
            TileUtil.TileCoord t = TileUtil.latLonToTile(lat, lon, z);
            for (TimePeriod p : TimePeriod.values()) {
                cache.evictTile(z, t.x(), t.y(), p);
            }
        }
        clusterCache.evictAll();
    }
}
