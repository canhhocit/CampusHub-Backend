package com.campushub.modules.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateWithdrawalConfigRequest(
        @NotNull(message = "Hạn mức rút tiền tối thiểu không được để trống")
        @DecimalMin(value = "1000.00", message = "Hạn mức rút tiền tối thiểu ít nhất là 1.000 VNĐ")
        BigDecimal minWithdrawalAmount
) {}
