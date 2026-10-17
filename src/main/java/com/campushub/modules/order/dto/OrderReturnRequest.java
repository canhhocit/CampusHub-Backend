package com.campushub.modules.order.dto;

import jakarta.validation.constraints.NotNull;

public class OrderReturnRequest {

    @NotNull(message = "Locker ID is required")
    private Long lockerId;

    @NotNull(message = "Compartment ID is required")
    private Long compartmentId;

    public Long getLockerId() {
        return lockerId;
    }

    public void setLockerId(Long lockerId) {
        this.lockerId = lockerId;
    }

    public Long getCompartmentId() {
        return compartmentId;
    }

    public void setCompartmentId(Long compartmentId) {
        this.compartmentId = compartmentId;
    }
}
