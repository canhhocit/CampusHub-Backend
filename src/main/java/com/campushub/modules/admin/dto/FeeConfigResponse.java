package com.campushub.modules.admin.dto;

import com.campushub.modules.admin.entity.FeeMode;
import java.math.BigDecimal;

public class FeeConfigResponse {
    private BigDecimal saleCommissionPercent;
    private FeeMode rentalFeeMode;
    private BigDecimal rentalFeeValue;

    public FeeConfigResponse() {
    }

    public FeeConfigResponse(BigDecimal saleCommissionPercent, FeeMode rentalFeeMode, BigDecimal rentalFeeValue) {
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
