package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maps OCRResponse in app/schemas/ocr.py from POST /api/v1/ocr.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OcrExtractionResponseDto {

    private boolean success;
    private CardSide side;

    @JsonProperty("processing_time_ms")
    private Double processingTimeMs;

    private Map<String, ExtractedFieldDto> fields;

    @JsonProperty("raw_detections")
    private List<RawDetectionDto> rawDetections;

    @JsonProperty("image_info")
    private ImageMetadataDto imageInfo;

    private List<String> warnings;
    private String disclaimer;

    public OcrExtractionResponseDto() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public CardSide getSide() {
        return side != null ? side : CardSide.UNKNOWN;
    }

    public void setSide(CardSide side) {
        this.side = side;
    }

    public Double getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Double processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Map<String, ExtractedFieldDto> getFields() {
        return fields != null ? fields : Collections.emptyMap();
    }

    public void setFields(Map<String, ExtractedFieldDto> fields) {
        this.fields = fields;
    }

    public List<RawDetectionDto> getRawDetections() {
        return rawDetections != null ? rawDetections : Collections.emptyList();
    }

    public void setRawDetections(List<RawDetectionDto> rawDetections) {
        this.rawDetections = rawDetections;
    }

    public ImageMetadataDto getImageInfo() {
        return imageInfo;
    }

    public void setImageInfo(ImageMetadataDto imageInfo) {
        this.imageInfo = imageInfo;
    }

    public List<String> getWarnings() {
        return warnings != null ? warnings : Collections.emptyList();
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }

    /**
     * Helper to safely extract field value by key name (e.g. "idNumber", "fullName").
     */
    public String getFieldValue(String fieldKey) {
        if (fields == null || fieldKey == null) {
            return null;
        }
        ExtractedFieldDto field = fields.get(fieldKey);
        return field != null ? field.getValue() : null;
    }

    /**
     * Helper to extract citizen ID (CCCD number) from field aliases.
     */
    public String getCitizenId() {
        String id = getFieldValue("id");
        if (id == null) id = getFieldValue("id_number");
        if (id == null) id = getFieldValue("idNumber");
        return id;
    }
}
