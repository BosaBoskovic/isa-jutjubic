package com.jutjubic.jutjubic_backend.dto;

import com.jutjubic.jutjubic_backend.model.PopularVideo;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PopularVideoDto {
    private Long videoId;
    private String title;
    private String authorUsername;
    private Integer popularityRank;
    private Double popularityScore;
    private Long viewsLast7Days;
    private LocalDateTime pipelineRunAt;

    public static PopularVideoDto from(PopularVideo pv) {
        PopularVideoDto dto = new PopularVideoDto();
        dto.setVideoId(pv.getVideoPost().getId());
        dto.setTitle(pv.getVideoPost().getTitle());
        dto.setAuthorUsername(pv.getVideoPost().getAuthor().getUsername());
        dto.setPopularityRank(pv.getPopularityRank());
        dto.setPopularityScore(pv.getPopularityScore());
        dto.setViewsLast7Days(pv.getViewsLast7Days());
        dto.setPipelineRunAt(pv.getPipelineRunAt());
        return dto;
    }
}