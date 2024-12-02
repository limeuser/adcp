package com.fishqq.adcp;

public class PoolMetric {
    private long borrowCount;
    private long timeoutCount;
    private long waitCount;
    private long badConnections;
    private long recycledConnections;
    private long createRawConnectionsCount;
    private long createRawConnectionTotalTimeMs;

    private long usedCount;
    private long usedTimeMs;

    public void recordCreateConnection(long spend) {
        ++this.createRawConnectionsCount;
        this.createRawConnectionTotalTimeMs += spend;
    }

    public void recordTimeout() {
        ++this.timeoutCount;
    }

    public void recordBorrow() {
        ++this.borrowCount;
    }

    public void recordBadConnection() {
        ++this.badConnections;
    }

    public void recordClosing(long usedTimeMs) {
        ++this.usedCount;
        this.usedTimeMs += usedTimeMs;
    }

    public void reset() {
        this.borrowCount = 0;
        this.timeoutCount = 0;
        this.waitCount = 0;
        this.badConnections = 0;
        this.recycledConnections = 0;
        this.createRawConnectionsCount = 0;
        this.usedCount = 0;
        this.usedTimeMs = 0;
    }

    public long getBorrowCount() {
        return borrowCount;
    }

    public long getTimeoutCount() {
        return timeoutCount;
    }

    public void recordRecycle(int closedConnections) {
        this.recycledConnections += closedConnections;
    }

    public long getWaitCount() {
        return waitCount;
    }

    public long getCreateRawConnectionsCount() {
        return createRawConnectionsCount;
    }

    public long getAvgUsedTime() {
        return usedCount == 0 ? 0 : usedTimeMs / usedCount;
    }

    public long getAvgCreateRawConnectionTime() {
        return createRawConnectionsCount == 0 ? 0 : createRawConnectionTotalTimeMs / createRawConnectionsCount;
    }

    @Override
    public String toString() {
        return "badConnections=" + badConnections +
                "\nborrowCount=" + borrowCount +
                "\nwaitCount=" + waitCount +
                "\ntimeoutCount=" + timeoutCount +
                "\nconnectionUsedCount=" + usedCount +
                "\navgConnectionUsedTimeMs=" + getAvgUsedTime() +
                "\nrecycledConnections=" + recycledConnections +
                "\ncreateRawConnectionsCount=" + createRawConnectionsCount +
                "\naveCreateRawConnectionsTimeMs=" + getAvgCreateRawConnectionTime();
    }
}
