package com.fishqq.adcp;

public class AdcpPoolConfig {
    private String poolName = "adcp-connection-pool";
    private int maxPoolSize = 32;
    private int minIdle = 2;
    private long borrowTimeoutMs = 30 * 1000;
    private int idleTimeoutSeconds = 10;
    private int recyclePeriodSeconds = 10;
    private int checkTimeoutSeconds = 10;
    private boolean validateConnection = false;
    private int maxLifetimeSeconds = 15 * 60;
    private int warnActiveTimeSeconds = 10;

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

    public boolean getValidateConnection() {
        return validateConnection;
    }

    public void setValidateConnection(boolean validateConnection) {
        this.validateConnection = validateConnection;
    }

    public int getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    public void setCheckTimeoutSeconds(int checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }

    public int getMaxLifetimeSeconds() {
        return maxLifetimeSeconds;
    }

    public void setMaxLifetimeSeconds(int maxLifetimeSeconds) {
        this.maxLifetimeSeconds = maxLifetimeSeconds;
    }

    public int getWarnActiveTimeSeconds() {
        return warnActiveTimeSeconds;
    }

    public void setWarnActiveTimeSeconds(int warnActiveTimeSeconds) {
        this.warnActiveTimeSeconds = warnActiveTimeSeconds;
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
                ", validateConnection=" + validateConnection +
                ", checkTimeoutSeconds=" + checkTimeoutSeconds +
                ", warnActiveTimeSecondgs=" + warnActiveTimeSeconds +
                ", maxLifetimeSeconds=" + maxLifetimeSeconds +
                '}';
    }
}
