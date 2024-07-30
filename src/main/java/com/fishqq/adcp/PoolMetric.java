package com.fishqq.adcp;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class PoolMetric {
    private long borrowCount;
    private long timeoutCount;
    private long recycleCount;
    private long waitCount;
    private long waitTimeMs;
    private long badConnections;
    private long recycledConnections;
    private long createRawConnectionsCount;
    private long closeConnectionsCountAfterCreate;
    private final AtomicLong waitingThreads = new AtomicLong(0);

    private int idleConnections;
    private int usingConnections;
    private long connectionUsingCount;
    private long connectionUsingTimeMs;
    private long rawConnectionCreateTimeMs;

    public void recordWaitConnection(long ms) {
        ++this.waitCount;
        waitTimeMs += ms;
    }

    public void recordCloseConnectionAfterCreate() {
        ++this.closeConnectionsCountAfterCreate;
    }

    public void recordBorrowStart() {
        this.waitingThreads.incrementAndGet();
    }

    public void recordBorrowEnd() {
        this.waitingThreads.decrementAndGet();
    }

    public void recordCreateConnection(long spend) {
        ++this.createRawConnectionsCount;
        this.rawConnectionCreateTimeMs += spend;
    }

    public void recordUsingNewConnection() {
        ++this.usingConnections;
    }

    public void recordTimeout() {
        ++this.timeoutCount;
    }

    public void recordBorrow() {
        ++this.borrowCount;

        --this.idleConnections;
        ++this.usingConnections;
    }

    public void recordBadConnection() {
        ++this.badConnections;
    }

    public void recordClosing(long spend) {
        --this.usingConnections;
        ++this.idleConnections;
        ++this.connectionUsingCount;
        connectionUsingTimeMs += spend;
    }

    public void reset() {
        this.borrowCount = 0;
        this.timeoutCount = 0;
        this.recycleCount = 0;
        this.waitCount = 0;
        this.waitTimeMs = 0;
        this.badConnections = 0;
        this.recycledConnections = 0;
        this.createRawConnectionsCount = 0;
        this.rawConnectionCreateTimeMs = 0;
        this.closeConnectionsCountAfterCreate = 0;
        this.connectionUsingCount = 0;
        this.connectionUsingTimeMs = 0;
    }

    public long getBorrowCount() {
        return borrowCount;
    }

    public long getTimeoutCount() {
        return timeoutCount;
    }

    public long getRecycleCount() {
        return recycleCount;
    }

    public void recordRecycle(int closedConnections) {
        this.recycledConnections += closedConnections;
        this.idleConnections -= closedConnections;
        ++this.recycleCount;
    }

    public long getWaitCount() {
        return waitCount;
    }

    public long getCreateRawConnectionsCount() {
        return createRawConnectionsCount;
    }

    public long getCloseConnectionsCountAfterCreate() {
        return closeConnectionsCountAfterCreate;
    }

    public int getIdleConnections() {
        return idleConnections;
    }

    public int getUsingConnections() {
        return usingConnections;
    }

    public long getWaitTimeMs() {
        return waitTimeMs;
    }

    public Optional<String> getWarning(WarningConfig config) {
        StringBuilder builder = new StringBuilder();
        long avgWaitTime = getAvgWaitTimeMs();
        long avgUsingTime = getConnectionUsingAvgTimeMs();
        long avgConnectionTime = getRawConnectionAvgCreateTimeMs();

        if (waitCount > 0 && avgWaitTime >= config.getAvgWaitTimeMs()) {
            builder.append(System.lineSeparator())
                    .append("thread avg wait time ")
                    .append(avgWaitTime)
                    .append(" > ")
                    .append(config.getAvgWaitTimeMs())
                    .append("ms");
        } else if (connectionUsingCount > 0 && avgUsingTime >= config.getAvgUsingTimeMs()) {
            builder.append(System.lineSeparator())
                    .append("thread avg execute sql time ")
                    .append(avgUsingTime)
                    .append(" > ")
                    .append(config.getAvgUsingTimeMs())
                    .append("ms");
        } else if (timeoutCount >= config.getWaitIdleTimeoutCount()) {
            builder.append(System.lineSeparator())
                    .append("thread wait idle connection timeout ")
                    .append(timeoutCount)
                    .append(" > ")
                    .append(config.getWaitIdleTimeoutCount())
                    .append("ms");
        } else if (createRawConnectionsCount > 0 && avgConnectionTime >= config.getAvgRawConnectionCreateTimeMs()) {
            builder.append(System.lineSeparator())
                    .append("create raw jdbc connection spend time ")
                    .append(avgConnectionTime)
                    .append(" > ")
                    .append(config.getAvgRawConnectionCreateTimeMs())
                    .append("ms");
        }

        if (builder.length() > 0) {
            return Optional.of(builder.toString());
        } else {
            return Optional.empty();
        }
    }

    public long getAvgWaitTimeMs() {
        return waitCount == 0 ? 0 : waitTimeMs / waitCount;
    }

    public long getConnectionUsingAvgTimeMs() {
        return connectionUsingTimeMs == 0 ? 0 : connectionUsingTimeMs / connectionUsingCount;
    }

    public long getRawConnectionAvgCreateTimeMs() {
        return createRawConnectionsCount == 0 ? 0 : rawConnectionCreateTimeMs / createRawConnectionsCount;
    }

    @Override
    public String toString() {
        return "\n idleConnections=" + idleConnections +
                "\n usingConnections=" + usingConnections +
                "\n waitingThreads=" + waitingThreads.get() +
                "\n waitCount=" + waitCount +
                "\n avgWaitTimeMs=" + getAvgWaitTimeMs() +
                "\n connectionUsingCount=" + connectionUsingCount +
                "\n connectionUsingTimeMs=" + getConnectionUsingAvgTimeMs() +
                "\n badConnections=" + badConnections +
                "\n borrowCount=" + borrowCount +
                "\n timeoutCount=" + timeoutCount +
                "\n recycleCount=" + recycleCount +
                "\n recycledConnections=" + recycledConnections +
                "\n createRawConnectionsCount=" + createRawConnectionsCount +
                "\n createRawConnectionsSpendMs=" + getRawConnectionAvgCreateTimeMs() +
                "\n closeConnectionsCountAfterCreate=" + closeConnectionsCountAfterCreate;
    }
}
