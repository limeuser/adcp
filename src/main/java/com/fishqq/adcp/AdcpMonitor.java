package com.fishqq.adcp;

public interface AdcpMonitor extends AutoCloseable {
    void initMetrics(AdcpMetrics metrics);

    void recordConnectionCreatedMs(long ms);

    void recordConnectionCreateError();

    void recordConnectionAcquiredMs(long ms);

    void recordConnectionUsageMs(long ms);

    void recordInvalidConnection();

    void recordConnectionTimeout();

    void recordWait(long ms);

    void reset();

    void close();
}
