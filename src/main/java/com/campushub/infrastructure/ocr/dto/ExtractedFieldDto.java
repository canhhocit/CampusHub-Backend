package com.campushub.infrastructure.ocr.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps ExtractedField in app/schemas/ocr.py.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtractedFieldDto {

    private String name;
    private String label;
    private String value;
    private FieldStatus status;
    private Double confidence;

    @JsonProperty("extraction_confidence")
    private Double extractionConfidence;

    @JsonProperty("raw_text")
    private String rawText;

    @JsonProperty("validation_message")
    private String validationMessage;

    public ExtractedFieldDto() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public FieldStatus getStatus() {
        return status != null ? status : FieldStatus.UNKNOWN;
    }

    public void setStatus(FieldStatus status) {
        this.status = status;
    }

    public boolean isExtracted() {
        return status != null && status.isExtracted();
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Double getExtractionConfidence() {
        return extractionConfidence;
    }

    public void setExtractionConfidence(Double extractionConfidence) {
        this.extractionConfidence = extractionConfidence;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
}
