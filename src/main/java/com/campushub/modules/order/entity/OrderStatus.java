package com.campushub.modules.order.entity;

public enum OrderStatus {
    PENDING_SELLER,
    AWAITING_DEPOSIT,
    READY_FOR_PICKUP,
    RENTING,
    AWAITING_RETURN_DEPOSIT,
    READY_FOR_RETURN_PICKUP,
    COMPLETED,
    CANCELLED,
    REJECTED,
    EXPIRED
}
