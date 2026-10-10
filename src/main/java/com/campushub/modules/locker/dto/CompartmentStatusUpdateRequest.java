package com.campushub.modules.locker.dto;

import com.campushub.modules.locker.entity.CompartmentStatus;
import jakarta.validation.constraints.NotNull;

public class CompartmentStatusUpdateRequest {

    @NotNull(message = "Compartment status is required")
    private CompartmentStatus status;

    public CompartmentStatus getStatus() {
        return status;
    }

    public void setStatus(CompartmentStatus status) {
        this.status = status;
    }
}
