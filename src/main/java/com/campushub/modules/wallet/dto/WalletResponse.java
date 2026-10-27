package com.campushub.modules.wallet.dto;

import java.math.BigDecimal;

public class WalletResponse {
    private Long userId;
    private String username;
    private BigDecimal balance;
    private BigDecimal frozenBalance;
    private BigDecimal totalBalance;

    public WalletResponse() {
    }

    public WalletResponse(Long userId, String username, BigDecimal balance, BigDecimal frozenBalance) {
        this.userId = userId;
        this.username = username;
        this.balance = balance != null ? balance : BigDecimal.ZERO;
        this.frozenBalance = frozenBalance != null ? frozenBalance : BigDecimal.ZERO;
        this.totalBalance = this.balance.add(this.frozenBalance);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getFrozenBalance() {
        return frozenBalance;
    }

    public void setFrozenBalance(BigDecimal frozenBalance) {
        this.frozenBalance = frozenBalance;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }
}
