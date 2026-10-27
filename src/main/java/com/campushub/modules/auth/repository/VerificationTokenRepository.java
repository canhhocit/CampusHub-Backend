package com.campushub.modules.auth.repository;

import com.campushub.modules.auth.entity.VerificationToken;
import com.campushub.common.enums.TokenType;
import com.campushub.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByTokenHashAndType(String tokenHash, TokenType type);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM VerificationToken v WHERE v.tokenHash = :hash AND v.type = :type")
    Optional<VerificationToken> findByTokenHashAndTypeWithLock(
            @org.springframework.data.repository.query.Param("hash") String hash,
            @org.springframework.data.repository.query.Param("type") TokenType type);
    
    @Modifying
    @Query("DELETE FROM VerificationToken v WHERE v.user = :user AND v.type = :type")
    void deleteByUserAndType(User user, TokenType type);

    @Modifying
    @Query("DELETE FROM VerificationToken v WHERE v.expiresAt < CURRENT_TIMESTAMP")
    void deleteExpiredTokens();
}
