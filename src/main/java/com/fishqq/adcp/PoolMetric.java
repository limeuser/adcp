package com.fishqq.adcp;

public class PoolMetric {
    private long borrowCount = 0;
    private long timeoutCount = 0;
    private long recycleCount = 0;
    private long waitCount = 0;
    private long badConnections;
    private long recycledConnections;
    private long createRawConnectionsCount = 0;
    private long closeConnectionsCountAfterCreate = 0;

    private int idleConnections;
    private int usingConnections;

    public void recordWaitConnection() {
        ++this.waitCount;
    }

    public void recordCloseConnectionAfterCreate() {
        ++this.closeConnectionsCountAfterCreate;
    }

    public void recordCreateConnection() {
        ++this.createRawConnectionsCount;
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

    public void recordClosing() {
        --this.usingConnections;
        ++this.idleConnections;
    }

    public void reset() {
        this.idleConnections = 0;
        this.usingConnections = 0;
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

    @Override
    public String toString() {
        return "PoolMetric{" +
                "idleConnections=" + idleConnections +
                ", usingConnections=" + usingConnections +
                ", badConnections=" + badConnections +
                ", borrowCount=" + borrowCount +
                ", waitCount=" + waitCount +
                ", timeoutCount=" + timeoutCount +
                ", recycleCount=" + recycleCount +
                ", recycledConnections=" + recycledConnections +
                ", createRawConnectionsCount=" + createRawConnectionsCount +
                ", closeConnectionsCountAfterCreate=" + closeConnectionsCountAfterCreate +
                '}';
    }
}
