package com.campushub.modules.locker.dto;

import jakarta.validation.constraints.NotBlank;

public class KioskDepositRequest {

    @NotBlank(message = "Mã đơn hàng không được để trống")
    private String orderNumber;

    public KioskDepositRequest() {}

    public KioskDepositRequest(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }
}

