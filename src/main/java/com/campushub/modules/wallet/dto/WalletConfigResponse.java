package com.campushub.modules.wallet.dto;

import java.math.BigDecimal;

public record WalletConfigResponse(
        BigDecimal minWithdrawalAmount
) {}

