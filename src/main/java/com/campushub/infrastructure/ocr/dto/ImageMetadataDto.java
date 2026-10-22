package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps ImageMetadata in Python app/schemas/ocr.py.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImageMetadataDto {

    private Integer width;
    private Integer height;
    private Integer channels;

    @JsonProperty("size_bytes")
    private Long sizeBytes;

    private String format;

    public ImageMetadataDto() {
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

    public Integer getChannels() {
        return channels;
    }

    public void setChannels(Integer channels) {
        this.channels = channels;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
