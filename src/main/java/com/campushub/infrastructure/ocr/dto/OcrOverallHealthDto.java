package com.campushub.infrastructure.ocr.dto;

/**
 * Aggregated health status of all 3 OCR & Biometrics subsystems:
 * 1. OCR Engine
 * 2. Face Verification (YuNet + SFace)
 * 3. Liveness Detection & Passive Anti-Spoofing (MiniFASNet dual scale)
 */
public class OcrOverallHealthDto {

    private boolean fullyReady;
    private OcrEngineHealthDto ocrEngine;
    private FaceHealthDto face;
    private LivenessHealthDto liveness;
    private String message;

    public OcrOverallHealthDto() {
    }

    public OcrOverallHealthDto(boolean fullyReady, OcrEngineHealthDto ocrEngine,
                               FaceHealthDto face, LivenessHealthDto liveness, String message) {
        this.fullyReady = fullyReady;
        this.ocrEngine = ocrEngine;
        this.face = face;
        this.liveness = liveness;
        this.message = message;
    }

    public boolean isFullyReady() {
        return fullyReady;
    }

    public void setFullyReady(boolean fullyReady) {
        this.fullyReady = fullyReady;
    }

    public OcrEngineHealthDto getOcrEngine() {
        return ocrEngine;
    }

    public void setOcrEngine(OcrEngineHealthDto ocrEngine) {
        this.ocrEngine = ocrEngine;
    }

    public FaceHealthDto getFace() {
        return face;
    }

    public void setFace(FaceHealthDto face) {
        this.face = face;
    }

    public LivenessHealthDto getLiveness() {
        return liveness;
    }

    public void setLiveness(LivenessHealthDto liveness) {
        this.liveness = liveness;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "OcrOverallHealthDto{" +
                "fullyReady=" + fullyReady +
                ", message='" + message + '\'' +
                ", ocrEngine=" + ocrEngine +
                ", face=" + face +
                ", liveness=" + liveness +
                '}';
    }
}
