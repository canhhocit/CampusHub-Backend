package com.campushub.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class OrderPickupRequest {

    @NotBlank(message = "Pickup code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pickup code must be 6 digits")
    private String pickupCode;

    public String getPickupCode() {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }
}
