package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps FaceBoundingBox in Python app/schemas/face.py.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaceBoundingBoxDto {

    private Integer x;
    private Integer y;
    private Integer width;
    private Integer height;
    private Double confidence;

    public FaceBoundingBoxDto() {
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
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

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }
}
