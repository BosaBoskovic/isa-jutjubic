package com.jutjubic.jutjubic_backend.dto;

import java.util.ArrayList;
import java.util.List;

public class MapTileResponseDto {

    private List<MapVideoDto> videos = new ArrayList<>();

    public MapTileResponseDto() {}

    public MapTileResponseDto(List<MapVideoDto> videos) {
        this.videos = videos;
    }

    public List<MapVideoDto> getVideos() {
        return videos;
    }

    public void setVideos(List<MapVideoDto> videos) {
        this.videos = videos;
    }
}
