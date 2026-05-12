package com.jutjubic.jutjubic_backend.dto;

import java.io.Serializable;

public class TranscodingRequest implements Serializable{

    private Long videoId;
    private String videoPath;

    public TranscodingRequest() {
    }

    public TranscodingRequest(Long videoId, String videoPath) {
        this.videoId = videoId;
        this.videoPath = videoPath;
    }

    public Long getVideoId() {
        return videoId;
    }

    public void setVideoId(Long videoId) {
        this.videoId = videoId;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }
}