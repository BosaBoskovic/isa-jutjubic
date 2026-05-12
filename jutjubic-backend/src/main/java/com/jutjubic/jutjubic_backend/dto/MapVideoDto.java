package com.jutjubic.jutjubic_backend.dto;

import com.jutjubic.jutjubic_backend.model.VideoPost;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MapVideoDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String thumbnailPath;
    private String videoPath;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private long viewsCount;
    private boolean cluster;
    private int count;

    public MapVideoDto() {}

    public MapVideoDto(Long id, String title, String thumbnailPath, String videoPath, Double latitude, Double longitude, LocalDateTime createdAt, long viewsCount) {
        this.id = id;
        this.title = title;
        this.thumbnailPath = thumbnailPath;
        this.videoPath = videoPath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
        this.viewsCount = viewsCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public String getVideoPath() { return videoPath; }
    public void setVideoPath(String videoPath) { this.videoPath = videoPath; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public long getViewsCount() { return viewsCount; }
    public void setViewsCount(long viewsCount) { this.viewsCount = viewsCount; }

    public boolean isCluster() {return cluster;}
    public void setCluster(boolean cluster) {this.cluster = cluster;}

    public int getCount() {return count;}
    public void setCount(int count) {this.count = count;}


    public static MapVideoDto from(VideoPost v) {
        MapVideoDto dto = new MapVideoDto();
        dto.id = v.getId();

        if (v.getLocation() != null) {
            dto.latitude = v.getLocation().getLatitude();
            dto.longitude = v.getLocation().getLongitude();
        }

        dto.title = v.getTitle();
        dto.thumbnailPath = v.getThumbnailPath();
        dto.videoPath = v.getVideoPath();
        dto.createdAt = v.getCreatedAt();
        dto.viewsCount = v.getViewsCount();

        return dto;
    }

}
