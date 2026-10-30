package com.campushub.modules.dispute.dto;

import com.campushub.modules.dispute.enums.DisputeReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DisputeCreateRequest {

    @NotNull(message = "Lý do khiếu nại không được để trống")
    private DisputeReason reason;

    @NotBlank(message = "Mô tả chi tiết sự cố không được để trống")
    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    @Size(max = 500, message = "Đường dẫn ảnh chứng cứ không vượt quá 500 ký tự")
    private String proofImageUrl;

    public DisputeCreateRequest() {}

    public DisputeReason getReason() {
        return reason;
    }

    public void setReason(DisputeReason reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProofImageUrl() {
        return proofImageUrl;
    }

    public void setProofImageUrl(String proofImageUrl) {
        this.proofImageUrl = proofImageUrl;
    }
}

