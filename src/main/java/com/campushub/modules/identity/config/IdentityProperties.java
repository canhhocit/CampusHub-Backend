package com.campushub.modules.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.identity")
public class IdentityProperties {

    private int maxDailyAttempts = 5;
    private int attemptTtlMinutes = 15;
    private int maxStepsPerAttempt = 10;

    public int getMaxDailyAttempts() {
        return maxDailyAttempts;
    }

    public void setMaxDailyAttempts(int maxDailyAttempts) {
        this.maxDailyAttempts = maxDailyAttempts;
    }

    public int getAttemptTtlMinutes() {
        return attemptTtlMinutes;
    }

    public void setAttemptTtlMinutes(int attemptTtlMinutes) {
        this.attemptTtlMinutes = attemptTtlMinutes;
    }

    public int getMaxStepsPerAttempt() {
        return maxStepsPerAttempt;
    }

    public void setMaxStepsPerAttempt(int maxStepsPerAttempt) {
        this.maxStepsPerAttempt = maxStepsPerAttempt;
    }
}
