package com.campushub.modules.order.dto;

public class OrderAcceptRequest {

    private Long lockerId;

    public Long getLockerId() {
        return lockerId;
    }

    public void setLockerId(Long lockerId) {
        this.lockerId = lockerId;
    }
}
