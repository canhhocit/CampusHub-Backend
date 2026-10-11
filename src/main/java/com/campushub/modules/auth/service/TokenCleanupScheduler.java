package com.campushub.modules.auth.service;

import com.campushub.modules.auth.repository.RefreshTokenRepository;
import com.campushub.modules.auth.repository.VerificationTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled job to clean up expired tokens from the database.
 * Runs daily at 3 AM to remove expired refresh tokens and verification tokens.
 */
@Component
public class TokenCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TokenCleanupScheduler.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;

    public TokenCleanupScheduler(
            RefreshTokenRepository refreshTokenRepository,
            VerificationTokenRepository verificationTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    /**
     * Clean up expired tokens daily at 3:00 AM
     * Cron format: second minute hour day month day-of-week
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        logger.info("Starting scheduled cleanup of expired tokens...");
        
        try {
            refreshTokenRepository.deleteExpiredTokens();
            logger.info("Deleted expired refresh tokens");
            
            verificationTokenRepository.deleteExpiredTokens();
            logger.info("Deleted expired verification tokens");
            
            logger.info("Token cleanup completed successfully");
        } catch (Exception e) {
            logger.error("Error during token cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Optional: Manual cleanup endpoint for admin testing
     * Can be called via a separate admin endpoint if needed
     */
    @Transactional
    public void forceCleanup() {
        logger.info("Manual token cleanup triggered");
        refreshTokenRepository.deleteExpiredTokens();
        verificationTokenRepository.deleteExpiredTokens();
        logger.info("Manual token cleanup completed");
    }
}
