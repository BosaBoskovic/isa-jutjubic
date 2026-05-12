package com.jutjubic.jutjubic_backend.service;

import com.jutjubic.jutjubic_backend.dto.MapTileResponseDto;
import com.jutjubic.jutjubic_backend.dto.MapVideoDto;
import com.jutjubic.jutjubic_backend.model.TimePeriod;
import com.jutjubic.jutjubic_backend.model.VideoPost;
import com.jutjubic.jutjubic_backend.repository.VideoPostRepository;
import com.jutjubic.jutjubic_backend.service.map.TileUtil;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator; // Dodato
import java.util.List;
import java.util.Collections; // Za prazne liste

@Service
public class MapTileService {

    private final VideoPostRepository repo;

    public MapTileService(VideoPostRepository repo) {
        this.repo = repo;
    }


    @Cacheable(value = "mapTiles", key = "{#z, #x, #y, #period}", unless = "#result.videos.isEmpty()")
    @Transactional(readOnly = true)
    public MapTileResponseDto getTile(int z, int x, int y, TimePeriod period) {
        TileUtil.Bounds b = TileUtil.tileToBounds(z, x, y);
        LocalDateTime fromDate = computeFromDate(period);

        List<VideoPost> posts = (fromDate == null)
                ? repo.findInBoundingBox(b.minLat(), b.maxLat(), b.minLng(), b.maxLng())
                : repo.findInBoundingBoxAndFromDate(b.minLat(), b.maxLat(), b.minLng(), b.maxLng(), fromDate);

        if (posts.isEmpty()) return new MapTileResponseDto(Collections.emptyList());

        if (z < 10) {
            VideoPost topVideo = posts.stream()
                    .max(Comparator.comparingLong(VideoPost::getViewsCount))
                    .orElse(posts.get(0));

            MapVideoDto dto = MapVideoDto.from(topVideo);
            dto.setCluster(true);
            dto.setCount(posts.size());

            if (topVideo.getLocation() != null) {
                dto.setLatitude(topVideo.getLocation().getLatitude());
                dto.setLongitude(topVideo.getLocation().getLongitude());
            }

            return new MapTileResponseDto(List.of(dto));
        }

        List<MapVideoDto> dtos = posts.stream().map(MapVideoDto::from).toList();
        return new MapTileResponseDto(dtos);
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
}