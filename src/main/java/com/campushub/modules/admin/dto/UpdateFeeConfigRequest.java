package com.campushub.modules.admin.dto;

import com.campushub.modules.admin.entity.FeeMode;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class UpdateFeeConfigRequest {

    @NotNull(message = "Tỷ lệ hoa hồng đơn bán không được để trống")
    @DecimalMin(value = "0.0", message = "Tỷ lệ hoa hồng phải >= 0")
    private BigDecimal saleCommissionPercent;

    @NotNull(message = "Chế độ tính phí thuê không được để trống")
    private FeeMode rentalFeeMode;

    @NotNull(message = "Giá trị phí thuê không được để trống")
    @DecimalMin(value = "0.0", message = "Giá trị phí thuê phải >= 0")
    private BigDecimal rentalFeeValue;

    public UpdateFeeConfigRequest() {
    }

    public UpdateFeeConfigRequest(BigDecimal saleCommissionPercent, FeeMode rentalFeeMode, BigDecimal rentalFeeValue) {
        this.saleCommissionPercent = saleCommissionPercent;
        this.rentalFeeMode = rentalFeeMode;
        this.rentalFeeValue = rentalFeeValue;
    }

    public BigDecimal getSaleCommissionPercent() {
        return saleCommissionPercent;
    }

    public void setSaleCommissionPercent(BigDecimal saleCommissionPercent) {
        this.saleCommissionPercent = saleCommissionPercent;
    }

    public FeeMode getRentalFeeMode() {
        return rentalFeeMode;
    }

    public void setRentalFeeMode(FeeMode rentalFeeMode) {
        this.rentalFeeMode = rentalFeeMode;
    }

    public BigDecimal getRentalFeeValue() {
        return rentalFeeValue;
    }

    public void setRentalFeeValue(BigDecimal rentalFeeValue) {
        this.rentalFeeValue = rentalFeeValue;
    }
}
