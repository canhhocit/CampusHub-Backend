package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Maps FaceDetectionResult in Python app/schemas/face.py.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FaceDetectionResultDto {

    private Boolean detected;
    private Integer count;

    @JsonProperty("image_width")
    private Integer imageWidth;

    @JsonProperty("image_height")
    private Integer imageHeight;

    private FaceBoundingBoxDto box;
    private Map<String, Object> landmarks;
    private FaceQualityCheckDto quality;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;

    public FaceDetectionResultDto() {
    }

    public Boolean getDetected() {
        return detected;
    }

    public void setDetected(Boolean detected) {
        this.detected = detected;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public FaceBoundingBoxDto getBox() {
        return box;
    }

    public void setBox(FaceBoundingBoxDto box) {
        this.box = box;
    }

    public Map<String, Object> getLandmarks() {
        return landmarks;
    }

    public void setLandmarks(Map<String, Object> landmarks) {
        this.landmarks = landmarks;
    }

    public FaceQualityCheckDto getQuality() {
        return quality;
    }

    public void setQuality(FaceQualityCheckDto quality) {
        this.quality = quality;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
