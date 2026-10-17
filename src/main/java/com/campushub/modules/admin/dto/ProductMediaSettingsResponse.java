package com.campushub.modules.admin.dto;

public class ProductMediaSettingsResponse {
    private Integer maxProductImages;
    private Integer maxProductVideos;
    private Boolean allowProductVideo;
    private Integer maxVideoSizeMb;

    public ProductMediaSettingsResponse() {}

    public ProductMediaSettingsResponse(Integer maxProductImages, Integer maxProductVideos, Boolean allowProductVideo, Integer maxVideoSizeMb) {
        this.maxProductImages = maxProductImages;
        this.maxProductVideos = maxProductVideos;
        this.allowProductVideo = allowProductVideo;
        this.maxVideoSizeMb = maxVideoSizeMb;
    }

    public Integer getMaxProductImages() {
        return maxProductImages;
    }

    public void setMaxProductImages(Integer maxProductImages) {
        this.maxProductImages = maxProductImages;
    }

    public Integer getMaxProductVideos() {
        return maxProductVideos;
    }

    public void setMaxProductVideos(Integer maxProductVideos) {
        this.maxProductVideos = maxProductVideos;
    }

    public Boolean getAllowProductVideo() {
        return allowProductVideo;
    }

    public void setAllowProductVideo(Boolean allowProductVideo) {
        this.allowProductVideo = allowProductVideo;
    }

    public Integer getMaxVideoSizeMb() {
        return maxVideoSizeMb;
    }

    public void setMaxVideoSizeMb(Integer maxVideoSizeMb) {
        this.maxVideoSizeMb = maxVideoSizeMb;
    }
}
