package com.fishqq.adcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class AdcpPool {
    private final Node idleHead;
    private final Node usingHead;
    private final Node emptyHead;

    private final DataSource dataSource;
    private final AdcpPoolConfig config;
    private final PoolMetric metric;
    private final SpinLock lock;

    private final static Logger logger = LoggerFactory.getLogger(AdcpPool.class);

    private static final class Node {
        private Node prev;
        private Node next;
        private ProxyConnection proxyConnection;
    }

    public AdcpPool(AdcpPoolConfig adcpPoolConfig, WarningConfig warningConfig, DataSource dataSource) {
        this.config = adcpPoolConfig;
        this.dataSource = dataSource;

        this.metric = new PoolMetric();

        this.lock = new SpinLock();

        this.idleHead = new Node();
        this.usingHead = new Node();
        this.emptyHead = new Node();

        List<Node> nodes = new ArrayList<>(adcpPoolConfig.getMaxPoolSize());
        for (int i = 0; i < adcpPoolConfig.getMaxPoolSize(); i++) {
            nodes.add(new Node());
        }

        this.emptyHead.next = nodes.get(0);
        Node prev = this.emptyHead;

        for (int i = 0; i < adcpPoolConfig.getMaxPoolSize() - 1; i++) {
            Node current = nodes.get(i);
            current.next = nodes.get(i + 1);
            current.prev = prev;
            prev = current;
        }

        nodes.get(nodes.size() - 1).prev = prev;

        Timer timer = new Timer("connection-recycle", true);

        if (config.getLogMetrics() || config.getLogWarning()) {
            timer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {
                            if (config.getLogMetrics()) {
                                logger.info("metrics:\n {}", metric);
                            }
                            if (config.getLogWarning()) {
                                Optional<String> warning = metric.getWarning(warningConfig);
                                warning.ifPresent(logger::warn);
                            }

                            lock.lock();
                            metric.reset();
                            lock.unlock();
                        }
                    },
                    TimeUnit.SECONDS.toMillis(config.getLogMetricsPeriodSeconds()),
                    TimeUnit.SECONDS.toMillis(config.getLogMetricsPeriodSeconds()));
        }

        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        if (metric.getIdleConnections() <= adcpPoolConfig.getMinIdle()) {
                            return;
                        }

                        List<Connection> closingConnections = new ArrayList<>();

                        lock.lock();

                        try {
                            int recycleCount = 0;
                            long timeoutMs = config.getIdleTimeoutSeconds() * 1000L;
                            int maxRecycleConnections = metric.getIdleConnections() - adcpPoolConfig.getMinIdle();

                            Node idle = idleHead.next;
                            while (idle != null && recycleCount < maxRecycleConnections) {
                                if (idle.proxyConnection.getIdleMs() >= timeoutMs) {
                                    closingConnections.add(idle.proxyConnection.rawConnection());
                                    idle.proxyConnection = null;

                                    Node removing = idle;
                                    idle = idle.next;
                                    moveNode(removing, emptyHead);
                                    ++recycleCount;
                                } else {
                                    idle = idle.next;
                                }
                            }

                            metric.recordRecycle(closingConnections.size());
                        } catch (Exception e) {
                            logger.error("recycle error", e);
                        } finally {
                            lock.unlock();
                        }

                        closingConnections.forEach(AdcpPool::closeJdbcConnection);
                    }
                },
                TimeUnit.SECONDS.toMillis(adcpPoolConfig.getRecyclePeriodSeconds()),
                TimeUnit.SECONDS.toMillis(adcpPoolConfig.getRecyclePeriodSeconds()));
    }

    public void shutdown() {
        lock.lock();

        closeAll(idleHead.next);
        closeAll(usingHead.next);

        metric.reset();

        lock.unlock();
    }

    private void closeAll(Node node) {
        Node first = node, last = node;

        while (node != null) {
            try {
                node.proxyConnection.rawConnection().close();
            } catch (Throwable e) {
                logger.error("close item error: " + node.proxyConnection, e);
            }

            last = node;
            node = node.next;
        }

        if (first != null) {
            Node firstEmpty = emptyHead.next;

            emptyHead.next = first;
            first.prev = emptyHead;
            last.next = firstEmpty;
            if (firstEmpty != null) {
                firstEmpty.prev = last;
            }
        }
    }

    public ProxyConnection borrow() throws SQLException {
        return borrow(config.getConnectionTimeoutMs());
    }

    public ProxyConnection borrow(long timeout) throws SQLException {
        long spendMs;
        long waitMs = timeout / 10;
        long startTime = System.currentTimeMillis();

        metric.recordBorrowStart();

        while (true) {
            Node item = tryBorrowFromIdle();

            // no idle connections
            if (item == null) {
                // no empty node, wait
                if (emptyHead.next == null) {
                    waitForEmptyNode(waitMs);
                } else {
                    Node node = createFromRawConnection();
                    if (node != null) {
                        metric.recordBorrowEnd();
                        return node.proxyConnection;
                    }
                }
            } else {
                if (isInvalid(item.proxyConnection)) {
                    logger.warn("jdbc connection is closed/invalid/lifetime timeout, try reconnect: {}",
                            item.proxyConnection.rawConnection());
                    cleanInvalidConnection(item);
                } else {
                    item.proxyConnection.initStatus();
                    metric.recordBorrowEnd();
                    return item.proxyConnection;
                }
            }

            spendMs = System.currentTimeMillis() - startTime;
            if (spendMs > timeout) {
                metric.recordBorrowEnd();
                metric.recordTimeout();

                throw new SQLTransientConnectionException(String.format(
                        "try get connection from pool timeout: after %d ms, max pool size: %d",
                        timeout,
                        config.getMaxPoolSize()));
            }
        }
    }

    private Node createFromRawConnection() throws SQLException {
        long start = System.currentTimeMillis();
        Connection connection = dataSource.getConnection();

        lock.lock();

        metric.recordCreateConnection(System.currentTimeMillis() - start);

        Node usingNode = moveNode(emptyHead.next, usingHead);

        if (usingNode == null) {
            metric.recordCloseConnectionAfterCreate();

            lock.unlock();

            logger.warn("double check empty node failed: many threads is get connection");

            try {
                connection.close();
            } catch (Throwable e) {
                logger.error("close connection error: " + connection, e);
            }

            return null;
        } else {
            usingNode.proxyConnection = new ProxyConnection(connection, () -> recycleAfterClosed(usingNode));

            metric.recordUsingNewConnection();

            lock.unlock();

            return usingNode;
        }
    }

    private void waitForEmptyNode(long waitMs) {
        try {
            Thread.sleep(waitMs);
            this.metric.recordWaitConnection(waitMs);
        } catch (InterruptedException e) {
            logger.error("pool get connection sleep is interrupted", e);
        }
    }

    private void cleanInvalidConnection(Node item) {
        closeJdbcConnection(item.proxyConnection.rawConnection());
        this.lockAndMoveNode(item, this.emptyHead);
        metric.recordBadConnection();
    }

    private static void closeJdbcConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error("close jdbc connection error: " + connection, e);
        }
    }

    private boolean isInvalid(ProxyConnection connection) {
        try {
            return connection.rawConnection().isClosed()
                    || (connection.getIdleMs() > config.getCheckPeriodSeconds() * 1000L
                    && !connection.rawConnection().isValid(config.getCheckTimeoutSeconds()))
                    || (connection.getLiveTime() >= config.getMaxLifetimeSeconds());
        } catch (SQLException e) {
            logger.error("check jdbc connection is closed/valid exception", e);
            return true;
        }
    }

    private Node tryBorrowFromIdle() {
        lock.lock();

        Node node = moveNode(idleHead.next, usingHead);

        if (node != null) {
            metric.recordBorrow();
        }

        lock.unlock();

        return node;
    }

    private void recycleAfterClosed(Node usingNode) {
        lock.lock();

        ProxyConnection proxyConnection = usingNode.proxyConnection;

        moveNode(usingNode, idleHead);

        metric.recordClosing(proxyConnection.getUsingTime());

        lock.unlock();
    }

    private void lockAndMoveNode(Node movingNode, Node toNode) {
        lock.lock();
        moveNode(movingNode, toNode);
        lock.unlock();
    }

    private Node moveNode(Node movingNode, Node toNode) {
        if (movingNode == null) {
            return null;
        }

        if (movingNode.next != null) {
            movingNode.next.prev = movingNode.prev;
        }
        if (movingNode.prev != null) {
            movingNode.prev.next = movingNode.next;
        }

        Node toNextNode = toNode.next;
        if (toNextNode != null) {
            toNextNode.prev = movingNode;
        }
        movingNode.next = toNextNode;

        movingNode.prev = toNode;
        toNode.next = movingNode;

        return movingNode;
    }

    public PoolMetric getMetric() {
        return metric;
    }

    public AdcpPoolConfig getConfig() {
        return config;
    }

    public DataSource getUnwrappedDataSource() {
        return this.dataSource;
    }

    @Override
    public String toString() {
        return "AdcpPool: " + config.getPoolName();
    }
}
