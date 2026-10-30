package com.campushub.modules.dispute.enums;

public enum DisputeStatus {
    OPEN("Đang chờ Admin xử lý"),
    RESOLVED_REFUND_BUYER("Đã giải quyết: Hoàn tiền cho Người mua"),
    RESOLVED_PAYOUT_SELLER("Đã giải quyết: Bác khiếu nại, giữ thanh toán cho Người bán"),
    CANCELLED("Người mua đã rút khiếu nại");

    private final String description;

    DisputeStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

