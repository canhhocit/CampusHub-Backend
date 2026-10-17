package com.campushub.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VerifyIdentityRequest {

    @NotBlank(message = "Số CCCD không được để trống")
    @Pattern(regexp = "^[0-9]{12}$", message = "Số CCCD phải gồm đúng 12 chữ số")
    private String citizenId;

    @NotBlank(message = "Ảnh mặt trước CCCD không được để trống")
    @jakarta.validation.constraints.Size(max = 500)
    @Pattern(regexp = "(?i)^https?://[^\\s]+$", message = "Image URL must use HTTP or HTTPS")
    private String idCardFrontUrl;

    @jakarta.validation.constraints.Size(max = 500)
    @Pattern(regexp = "(?i)^https?://[^\\s]+$", message = "Image URL must use HTTP or HTTPS")
    private String idCardBackUrl;

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getIdCardFrontUrl() {
        return idCardFrontUrl;
    }

    public void setIdCardFrontUrl(String idCardFrontUrl) {
        this.idCardFrontUrl = idCardFrontUrl;
    }

    public String getIdCardBackUrl() {
        return idCardBackUrl;
    }

    public void setIdCardBackUrl(String idCardBackUrl) {
        this.idCardBackUrl = idCardBackUrl;
    }
}
