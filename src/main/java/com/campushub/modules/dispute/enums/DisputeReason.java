package com.campushub.modules.dispute.enums;

public enum DisputeReason {
    EMPTY_COMPARTMENT("Ngăn tủ bị rỗng / không có hàng"),
    DAMAGED_ITEM("Hàng hóa bị hư hại / hỏng hóc"),
    WRONG_ITEM("Hàng hóa sai chủng loại / không đúng mô tả"),
    COUNTERFEIT("Nghi vấn hàng giả / gian lận"),
    OTHER("Lý do khác");

    private final String description;

    DisputeReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

