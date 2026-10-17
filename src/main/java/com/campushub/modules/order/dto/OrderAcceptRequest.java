package com.campushub.modules.order.dto;

public class OrderAcceptRequest {

    @jakarta.validation.constraints.Positive
    private Long lockerId;

    public Long getLockerId() {
        return lockerId;
    }

    public void setLockerId(Long lockerId) {
        this.lockerId = lockerId;
    }
}
