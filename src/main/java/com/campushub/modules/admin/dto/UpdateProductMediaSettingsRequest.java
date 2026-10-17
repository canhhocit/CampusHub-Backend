package com.campushub.modules.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateProductMediaSettingsRequest {

    @NotNull
    @Min(1)
    @Max(20)
    private Integer maxProductImages;

    @NotNull
    @Min(0)
    @Max(5)
    private Integer maxProductVideos;

    @NotNull
    private Boolean allowProductVideo;

    @NotNull
    @Min(1)
    @Max(200)
    private Integer maxVideoSizeMb;

    public UpdateProductMediaSettingsRequest() {}

    public UpdateProductMediaSettingsRequest(Integer maxProductImages, Integer maxProductVideos, Boolean allowProductVideo, Integer maxVideoSizeMb) {
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
