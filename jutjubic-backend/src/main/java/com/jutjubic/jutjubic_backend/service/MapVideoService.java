package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.dto.MapVideoDto;
import com.jutjubic.jutjubic_backend.model.TimePeriod;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.service.map.TileUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MapVideoService {

    private final VideoPostRepository repo;

    public MapVideoService(VideoPostRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = "mapClusters",
            condition = "#zoom < 12",
            key = "T(com.jutjubic.jutjubic_backend.service.MapVideoService).viewportKey(#minLat,#maxLat,#minLng,#maxLng,#zoom,#period)"
    )
    public List<MapVideoDto> getMapVideos(double minLat, double maxLat, double minLng, double maxLng, int zoom, TimePeriod period) {
        zoom = Math.max(0, Math.min(zoom, 20));
        LocalDateTime fromDate = computeFromDate(period);

        List<VideoPost> posts;

        if (fromDate != null) {
            posts = repo.findInBoundingBoxAndFromDate(minLat, maxLat, minLng, maxLng, fromDate);
        } else {
            posts = repo.findInBoundingBox(minLat, maxLat, minLng, maxLng);
        }

        if (zoom >= 12) {
            return posts.stream()
                    .filter(v -> v.getLocation() != null)
                    .map(MapVideoDto::from)
                    .toList();
        }

       return groupByTiles(posts, zoom);

    }


    public static String cacheKey(double minLat, double maxLat, double minLng, double maxLng, int zoom, TimePeriod period) {
        int z = Math.max(0, Math.min(zoom, 20));

        double centerLat = (minLat + maxLat) / 2.0;
        double centerLng = (minLng + maxLng) / 2.0;

        int x = TileUtil.lonToTileX(centerLng, z);
        int y = TileUtil.latToTileY(centerLat, z);

        return "z:" + z + ":x:" + x + ":y:" + y + ":p:" + (period == null ? "ALL" : period.name());
    }

    private LocalDateTime computeFromDate(TimePeriod period) {
        LocalDateTime now = LocalDateTime.now();

        if (period == null || period == TimePeriod.ALL) return null;

        return switch (period) {
            case LAST_30_DAYS -> now.minusDays(30);
            case THIS_YEAR -> LocalDateTime.of(now.getYear(), 1, 1, 0, 0);
            case ALL -> null;
        };
    }

    private String computeTileId(VideoPost v, int zoom) {
        if (v.getLocation() == null) return "no_location";

        double lat = v.getLocation().getLatitude();
        double lng = v.getLocation().getLongitude();

        double tileSize;

        if (zoom >= 12) {
            tileSize = 0.2;
        } else if (zoom >= 7) {
            tileSize = 1.0;
        } else {
            tileSize = 5.0;
        }

        int tileX = (int) Math.floor(lng / tileSize);
        int tileY = (int) Math.floor(lat / tileSize);

        return "z" + zoom + "_x" + tileX + "_y" + tileY;
    }


    private List<MapVideoDto> groupByTiles(List<VideoPost> posts, int zoom) {

        Map<String, List<VideoPost>> tiles = posts.stream()
                .filter(v -> v.getLocation() != null)
                .collect(Collectors.groupingBy(v -> computeTileId(v, zoom)));

        List<MapVideoDto> result = new ArrayList<>();

        for (List<VideoPost> tilePosts : tiles.values()) {
            VideoPost representative = tilePosts.stream()
                    .max((a, b) -> Long.compare(a.getViewsCount(), b.getViewsCount()))
                    .orElse(tilePosts.get(0));


            MapVideoDto dto = MapVideoDto.from(representative);

            dto.setTitle("Videos: " + tilePosts.size());

            dto.setCluster(true);
            dto.setCount(tilePosts.size());

            result.add(dto);
        }

        return result;
    }

    public static String viewportKey(double minLat, double maxLat, double minLng, double maxLng, int zoom, TimePeriod period) {
        int z = Math.max(0, Math.min(zoom, 20));

        double a = round(minLat, 3);
        double b = round(maxLat, 3);
        double c = round(minLng, 3);
        double d = round(maxLng, 3);

        String p = (period == null) ? "ALL" : period.name();
        return "vp:z:" + z + ":a:" + a + ":b:" + b + ":c:" + c + ":d:" + d + ":p:" + p;
    }

    private static double round(double v, int decimals) {
        double m = Math.pow(10, decimals);
        return Math.round(v * m) / m;
    }


}
