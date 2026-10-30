package com.campushub.modules.locker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class KioskPickupRequest {

    @NotBlank(message = "Mã PIN không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã PIN phải bao gồm đúng 6 chữ số")
    private String pinCode;

    public KioskPickupRequest() {}

    public KioskPickupRequest(String pinCode) {
        this.pinCode = pinCode;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }
}

