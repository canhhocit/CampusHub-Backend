package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Maps RawDetection in Python app/schemas/ocr.py.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawDetectionDto {

    private String text;
    private Double confidence;
    private List<List<Double>> box;

    public RawDetectionDto() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public List<List<Double>> getBox() {
        return box;
    }

    public void setBox(List<List<Double>> box) {
        this.box = box;
    }
}
