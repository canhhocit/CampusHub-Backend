package com.campushub.modules.wallet.repository;

import com.campushub.modules.wallet.entity.UserBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBankAccountRepository extends JpaRepository<UserBankAccount, Long> {

    List<UserBankAccount> findByUserId(Long userId);

    Optional<UserBankAccount> findByUserIdAndIsDefaultTrue(Long userId);

    Optional<UserBankAccount> findByIdAndUserId(Long id, Long userId);
}
