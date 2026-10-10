package com.campushub.modules.order.dto;

import java.time.LocalDateTime;

public class OrderDetailResponse extends OrderResponse {

    private LocalDateTime pickupDeadline;
    private Integer pickupAttempts;
    private LocalDateTime depositAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime cancelledAt;
    private String rejectionReason;
    private Long version;
    private String pickupCode;

    public LocalDateTime getPickupDeadline() {
        return pickupDeadline;
    }

    public void setPickupDeadline(LocalDateTime pickupDeadline) {
        this.pickupDeadline = pickupDeadline;
    }

    public Integer getPickupAttempts() {
        return pickupAttempts;
    }

    public void setPickupAttempts(Integer pickupAttempts) {
        this.pickupAttempts = pickupAttempts;
    }

    public LocalDateTime getDepositAt() {
        return depositAt;
    }

    public void setDepositAt(LocalDateTime depositAt) {
        this.depositAt = depositAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getPickupCode() {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }
}
