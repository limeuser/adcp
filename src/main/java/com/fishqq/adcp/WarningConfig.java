package com.fishqq.adcp;

public class WarningConfig {
    private long avgWaitTimeMs = 1000;
    private long avgRawConnectionCreateTimeMs = 1000;
    private int waitIdleTimeoutCount = 1;
    private long avgUsingTimeMs = 3000;

    public long getAvgWaitTimeMs() {
        return avgWaitTimeMs;
    }

    public void setAvgWaitTimeMs(long avgWaitTimeMs) {
        this.avgWaitTimeMs = avgWaitTimeMs;
    }

    public long getAvgRawConnectionCreateTimeMs() {
        return avgRawConnectionCreateTimeMs;
    }

    public void setAvgRawConnectionCreateTimeMs(long avgRawConnectionCreateTimeMs) {
        this.avgRawConnectionCreateTimeMs = avgRawConnectionCreateTimeMs;
    }

    public int getWaitIdleTimeoutCount() {
        return waitIdleTimeoutCount;
    }

    public void setWaitIdleTimeoutCount(int waitIdleTimeoutCount) {
        this.waitIdleTimeoutCount = waitIdleTimeoutCount;
    }

    public long getAvgUsingTimeMs() {
        return avgUsingTimeMs;
    }

    public void setAvgUsingTimeMs(long avgUsingTimeMs) {
        this.avgUsingTimeMs = avgUsingTimeMs;
    }
}
