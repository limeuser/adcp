package com.fishqq.adcp;

public class AdcpPoolConfig {
    private String poolName = "adcp-connection-pool";
    private int maxPoolSize = 32;
    private int minIdle = 2;
    private long connectionTimeoutMs = 30 * 1000;
    private int idleTimeoutSeconds = 3 * 60;
    private int recyclePeriodSeconds = 10;
    private int checkPeriodSeconds = 60;
    private int checkTimeoutSeconds = 10;

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

    public long getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
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

    public int getCheckPeriodSeconds() {
        return checkPeriodSeconds;
    }

    public void setCheckPeriodSeconds(int checkPeriodSeconds) {
        this.checkPeriodSeconds = checkPeriodSeconds;
    }

    public int getCheckTimeoutSeconds() {
        return checkTimeoutSeconds;
    }

    public void setCheckTimeoutSeconds(int checkTimeoutSeconds) {
        this.checkTimeoutSeconds = checkTimeoutSeconds;
    }
}
