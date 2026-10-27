package com.campushub.modules.wallet.entity;

import com.campushub.common.model.BaseEntity;
import com.campushub.modules.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "frozen_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal frozenBalance = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    public Wallet() {
    }

    public Wallet(User user) {
        this.user = user;
        this.balance = BigDecimal.ZERO;
        this.frozenBalance = BigDecimal.ZERO;
        this.version = 0;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
