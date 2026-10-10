package com.campushub.modules.locker.dto;

import com.campushub.modules.locker.entity.LockerStatus;
import jakarta.validation.constraints.NotNull;

public class LockerStatusUpdateRequest {

    @NotNull(message = "Locker status is required")
    private LockerStatus status;

    public LockerStatus getStatus() {
        return status;
    }

    public void setStatus(LockerStatus status) {
        this.status = status;
    }
}
