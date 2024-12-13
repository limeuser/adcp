package com.fishqq.adcp;

public class AdcpPoolConfig {
    private String poolName = "adcp-connection-pool";
    private int maxPoolSize = 32;
    private int minIdle = 2;
    private long borrowTimeoutMs = 30 * 1000;
    private int idleTimeoutSeconds = 180;
    private int recyclePeriodSeconds = 60;
    private int checkValidationTimeoutSeconds = 10;
    private long aliveCheckPeriodMs = 500;
    private int maxLifetimeSeconds = 15 * 60;
    private int leakDetectionThresholdSeconds = 60;

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public long getBorrowTimeoutMs() {
        return borrowTimeoutMs;
    }

    public void setBorrowTimeoutMs(long borrowTimeoutMs) {
        this.borrowTimeoutMs = borrowTimeoutMs;
    }

    public int getIdleTimeoutSeconds() {
        return idleTimeoutSeconds;
    }

    public void setIdleTimeoutSeconds(int idleTimeoutSeconds) {
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public int getRecyclePeriodSeconds() {
        return recyclePeriodSeconds;
    }

    public void setRecyclePeriodSeconds(int recyclePeriodSeconds) {
        this.recyclePeriodSeconds = recyclePeriodSeconds;
    }

    public long getAliveCheckPeriodMs() {
        return aliveCheckPeriodMs;
    }

    public void setAliveCheckPeriodMs(long aliveCheckPeriodMs) {
        this.aliveCheckPeriodMs = aliveCheckPeriodMs;
    }

    public int getCheckValidationTimeoutSeconds() {
        return checkValidationTimeoutSeconds;
    }

    public void setCheckValidationTimeoutSeconds(int checkValidationTimeoutSeconds) {
        this.checkValidationTimeoutSeconds = checkValidationTimeoutSeconds;
    }

    public int getMaxLifetimeSeconds() {
        return maxLifetimeSeconds;
    }

    public void setMaxLifetimeSeconds(int maxLifetimeSeconds) {
        this.maxLifetimeSeconds = maxLifetimeSeconds;
    }

    public int getLeakDetectionThresholdSeconds() {
        return leakDetectionThresholdSeconds;
    }

    public void setLeakDetectionThresholdSeconds(int leakDetectionThresholdSeconds) {
        this.leakDetectionThresholdSeconds = leakDetectionThresholdSeconds;
    }

    @Override
    public String toString() {
        return "AdcpPoolConfig{" +
                "poolName='" + poolName + '\'' +
                ", maxPoolSize=" + maxPoolSize +
                ", minIdle=" + minIdle +
                ", borrowTimeoutMs=" + borrowTimeoutMs +
                ", idleTimeoutSeconds=" + idleTimeoutSeconds +
                ", recyclePeriodSeconds=" + recyclePeriodSeconds +
                ", aliveCheckPeriodMs=" + aliveCheckPeriodMs +
                ", checkValidationTimeoutSeconds=" + checkValidationTimeoutSeconds +
                ", leakDetectionThresholdSeconds=" + leakDetectionThresholdSeconds +
                ", maxLifetimeSeconds=" + maxLifetimeSeconds +
                '}';
    }
}
