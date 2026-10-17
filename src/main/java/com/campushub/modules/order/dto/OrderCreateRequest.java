package com.campushub.modules.order.dto;

import jakarta.validation.constraints.NotNull;

public class OrderCreateRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long preferredLockerId;

    private com.campushub.modules.order.entity.OrderType orderType = com.campushub.modules.order.entity.OrderType.PURCHASE;

    private Integer rentalDurationDays;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getPreferredLockerId() {
        return preferredLockerId;
    }

    public void setPreferredLockerId(Long preferredLockerId) {
        this.preferredLockerId = preferredLockerId;
    }

    public com.campushub.modules.order.entity.OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(com.campushub.modules.order.entity.OrderType orderType) {
        this.orderType = orderType;
    }

    public Integer getRentalDurationDays() {
        return rentalDurationDays;
    }

    public void setRentalDurationDays(Integer rentalDurationDays) {
        this.rentalDurationDays = rentalDurationDays;
    }
}
