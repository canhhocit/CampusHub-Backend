package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Maps FaceQualityCheck in Python app/schemas/face.py.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaceQualityCheckDto {

    @JsonProperty("is_valid")
    private Boolean valid;

    private Integer width;
    private Integer height;
    private Double brightness;

    @JsonProperty("blur_score")
    private Double blurScore;

    @JsonProperty("aspect_ratio")
    private Double aspectRatio;

    private List<String> warnings;

    public FaceQualityCheckDto() {
    }

    public Boolean getValid() {
        return valid;
    }

    public void setValid(Boolean valid) {
        this.valid = valid;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Double getBrightness() {
        return brightness;
    }

    public void setBrightness(Double brightness) {
        this.brightness = brightness;
    }

    public Double getBlurScore() {
        return blurScore;
    }

    public void setBlurScore(Double blurScore) {
        this.blurScore = blurScore;
    }

    public Double getAspectRatio() {
        return aspectRatio;
    }

    public void setAspectRatio(Double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public List<String> getWarnings() {
        return warnings != null ? warnings : Collections.emptyList();
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
