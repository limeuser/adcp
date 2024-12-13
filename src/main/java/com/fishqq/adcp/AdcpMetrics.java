package com.fishqq.adcp;

public interface AdcpMetrics {
    int getTotalConnectionCount();

    int getIdleConnectionCount();

    int getActiveConnectionCount();

    int getMaxConnectionCount();

    int getMinConnectionCount();

    int getPendingThreadCount();
}
