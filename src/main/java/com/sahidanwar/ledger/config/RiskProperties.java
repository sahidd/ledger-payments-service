package com.sahidanwar.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.risk")
public class RiskProperties {

    private BigDecimal unusualAmount = new BigDecimal("5000.00");
    private int velocityLimit = 5;
    private long velocityWindowSeconds = 600;
    private BigDecimal averageMultiplier = new BigDecimal("5");
    private int minimumSample = 3;

    public BigDecimal getUnusualAmount() {
        return unusualAmount;
    }

    public void setUnusualAmount(BigDecimal unusualAmount) {
        this.unusualAmount = unusualAmount;
    }

    public int getVelocityLimit() {
        return velocityLimit;
    }

    public void setVelocityLimit(int velocityLimit) {
        this.velocityLimit = velocityLimit;
    }

    public long getVelocityWindowSeconds() {
        return velocityWindowSeconds;
    }

    public void setVelocityWindowSeconds(long velocityWindowSeconds) {
        this.velocityWindowSeconds = velocityWindowSeconds;
    }

    public BigDecimal getAverageMultiplier() {
        return averageMultiplier;
    }

    public void setAverageMultiplier(BigDecimal averageMultiplier) {
        this.averageMultiplier = averageMultiplier;
    }

    public int getMinimumSample() {
        return minimumSample;
    }

    public void setMinimumSample(int minimumSample) {
        this.minimumSample = minimumSample;
    }
}
