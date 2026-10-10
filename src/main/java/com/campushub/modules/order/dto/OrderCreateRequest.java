package com.campushub.modules.order.dto;

import jakarta.validation.constraints.NotNull;

public class OrderCreateRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long preferredLockerId;

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
}
