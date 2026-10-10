package com.campushub.modules.locker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CompartmentCreateRequest {

    @NotBlank(message = "Compartment code is required")
    @Size(max = 50, message = "Compartment code cannot exceed 50 characters")
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
