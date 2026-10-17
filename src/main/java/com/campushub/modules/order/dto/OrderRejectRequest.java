package com.campushub.modules.order.dto;

public class OrderRejectRequest {

    @jakarta.validation.constraints.Size(max = 255)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
