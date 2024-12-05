package com.fishqq.adcp;

import java.util.concurrent.atomic.AtomicLong;

public class AdcpMemoryMonitor implements AdcpMonitor {
    private final AtomicLong acquireCount = new AtomicLong(0);
    private final AtomicLong acquireTimeMs = new AtomicLong(0);
    private final AtomicLong timeoutCount = new AtomicLong(0);
    private final AtomicLong waitCount = new AtomicLong(0);
    private final AtomicLong waitTimeMs = new AtomicLong(0);
    private final AtomicLong connectionCreateError = new AtomicLong(0);
    private final AtomicLong invalidConnectionCount = new AtomicLong(0);
    private final AtomicLong createRawConnectionsCount = new AtomicLong(0);
    private final AtomicLong createRawConnectionTotalTimeMs = new AtomicLong(0);

    private final AtomicLong usedCount = new AtomicLong(0);
    private final AtomicLong usedTimeMs = new AtomicLong(0);

    @Override
    public void reset() {
        this.acquireCount.set(0);
        this.acquireTimeMs.set(0);
        this.timeoutCount.set(0);
        this.waitCount.set(0);
        this.waitTimeMs.set(0);
        this.connectionCreateError.set(0);
        this.createRawConnectionsCount.set(0);
        this.createRawConnectionTotalTimeMs.set(0);
        this.invalidConnectionCount.set(0);
        this.usedCount.set(0);
        this.usedTimeMs.set(0);
    }

    public long getAvgUsedTime() {
        return usedCount.get() == 0 ? 0 : usedTimeMs.get() / usedCount.get();
    }

    public long getAvgCreateRawConnectionTime() {
        return createRawConnectionsCount.get() == 0 ? 0 :
                createRawConnectionTotalTimeMs.get() / createRawConnectionsCount.get();
    }

    @Override
    public void initMetrics(AdcpMetrics metrics) {

    }

    @Override
    public void recordConnectionCreatedMs(long ms) {
        createRawConnectionsCount.incrementAndGet();
        createRawConnectionTotalTimeMs.addAndGet(ms);
    }

    @Override
    public void recordConnectionCreateError() {
        connectionCreateError.incrementAndGet();
    }

    @Override
    public void recordConnectionAcquiredMs(long ms) {
        acquireCount.incrementAndGet();
        acquireTimeMs.addAndGet(ms);
    }

    public long getAvgAcquireTimeMs() {
        return acquireCount.get() == 0 ? 0 : acquireTimeMs.get() / acquireCount.get();
    }

    @Override
    public void recordConnectionUsageMs(long ms) {
        usedCount.incrementAndGet();
        usedTimeMs.addAndGet(ms);
    }

    @Override
    public void recordInvalidConnection() {
        invalidConnectionCount.incrementAndGet();
    }

    @Override
    public void recordConnectionTimeout() {
        timeoutCount.incrementAndGet();
    }

    @Override
    public void recordWait(long ms) {
        waitCount.incrementAndGet();
        waitTimeMs.addAndGet(ms);
    }

    public long getAvgWaitMs() {
        return waitCount.get() == 0 ? 0 : waitTimeMs.get() / waitCount.get();
    }

    @Override
    public void close() {
        reset();
    }

    @Override
    public String toString() {
        return "connectionCreateError=" + connectionCreateError.get() +
                "\nacquireCount=" + acquireCount.get() +
                "\navgAcquireTimeMs=" + getAvgAcquireTimeMs() +
                "\nwaitCount=" + waitCount.get() +
                "\navgWaitTimeMs=" + getAvgWaitMs() +
                "\ntimeoutCount=" + timeoutCount.get() +
                "\ninvalidConnectionCount=" + invalidConnectionCount.get() +
                "\nconnectionUsedCount=" + usedCount.get() +
                "\navgConnectionUsedTimeMs=" + getAvgUsedTime() +
                "\ncreateRawConnectionsCount=" + createRawConnectionsCount.get() +
                "\naveCreateRawConnectionsTimeMs=" + getAvgCreateRawConnectionTime();
    }
}
