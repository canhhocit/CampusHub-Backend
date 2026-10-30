package com.campushub.modules.admin.dto;

import java.math.BigDecimal;

public record WithdrawalConfigResponse(
        BigDecimal minWithdrawalAmount
) {}

