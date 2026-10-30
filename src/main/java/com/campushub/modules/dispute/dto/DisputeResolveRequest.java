package com.campushub.modules.dispute.dto;

import com.campushub.modules.dispute.enums.DisputeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DisputeResolveRequest {

    @NotNull(message = "Kết quả giải quyết không được để trống")
    private DisputeStatus resolution; // RESOLVED_REFUND_BUYER or RESOLVED_PAYOUT_SELLER

    @Size(max = 1000, message = "Ghi chú của admin không vượt quá 1000 ký tự")
    private String adminNote;

    public DisputeResolveRequest() {}

    public DisputeStatus getResolution() {
        return resolution;
    }

    public void setResolution(DisputeStatus resolution) {
        this.resolution = resolution;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }
}

