package com.campushub.modules.identity.enums;

public enum VerificationAttemptStatus {
    STARTED,
    LIVENESS_IN_PROGRESS,
    LIVENESS_PASSED,
    FACE_VERIFICATION_PASSED,
    PENDING_REVIEW,
    REJECTED,
    EXPIRED;

    public boolean isTerminal() {
        return this == PENDING_REVIEW || this == REJECTED || this == EXPIRED;
    }

    public boolean canPerformLiveness() {
        return this == LIVENESS_IN_PROGRESS;
    }

    public boolean canVerifyFace() {
        return this == LIVENESS_PASSED;
    }
}
