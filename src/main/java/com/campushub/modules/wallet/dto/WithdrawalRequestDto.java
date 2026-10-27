package com.campushub.modules.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class WithdrawalRequestDto {

    @NotNull(message = "Số tiền rút không được để trống")
    @DecimalMin(value = "10000.00", message = "Số tiền rút tối thiểu là 10,000 VNĐ")
    private BigDecimal amount;

    private Long bankAccountId;

    public WithdrawalRequestDto() {
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }
}
