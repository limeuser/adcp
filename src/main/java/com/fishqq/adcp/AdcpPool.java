package com.fishqq.adcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AdcpPool implements AdcpMetrics {
    private final Pool pool;

    private final DataSource dataSource;
    private final AdcpPoolConfig config;
    private final AdcpMonitor monitor;

    private volatile boolean closed;
    private final TimerTask recycleTask;

    private final AtomicLong connectionId = new AtomicLong(0);
    private final Map<Thread, Long> pendingThreads = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(AdcpPool.class);

    public AdcpPool(AdcpPoolConfig adcpPoolConfig, DataSource dataSource, AdcpMonitor monitor) {
        this.config = adcpPoolConfig;
        this.dataSource = dataSource;
        this.monitor = monitor;
        this.pool = new Pool(config.getMaxPoolSize());

        this.recycleTask = new TimerTask() {
            @Override
            public void run() {
                logger.info("idle: {}, active: {}, metrics\n{}", pool.idleCount(), pool.activeCount(), monitor);
                monitor.reset();

                if (closed || pool.idleCount() <= adcpPoolConfig.getMinIdle() || !pendingThreads.isEmpty()) {
                    return;
                }

                long timeoutMs = config.getIdleTimeoutSeconds() * 1000L;
                long maxLifetime = config.getMaxLifetimeSeconds() * 1000L;
                long now = System.currentTimeMillis();

                List<Connection> recycledConnections = new ArrayList<>();

                pool.recycleIdleItems(
                        recycledConnections::add,
                        (conn) -> !closed && (conn.getIdleMs() >= timeoutMs
                                || now - conn.getCreatedAt() >= maxLifetime),
                        () -> closed || pool.idleCount() <= config.getMinIdle() || !pendingThreads.isEmpty());

                if (!recycledConnections.isEmpty()) {
                    logger.info("recycled {} idle connections", recycledConnections.size());
                }

                recycledConnections.forEach(AdcpPool::closeJdbcConnection);
            }
        };

        Timer timer = new Timer("connection-recycle", true);
        timer.schedule(this.recycleTask,
                TimeUnit.SECONDS.toMillis(config.getRecyclePeriodSeconds()),
                TimeUnit.SECONDS.toMillis(config.getRecyclePeriodSeconds()));
    }

    public void shutdown() {
        closed = true;

        recycleTask.cancel();

        pool.destroyAll(connection -> {
            try {
                connection.rawConnection().close();
            } catch (Throwable e) {
                logger.error("close item error: {}", connection, e);
            }
        });

        pendingThreads.clear();

        monitor.close();
    }

    public ProxyConnection borrow() throws SQLException {
        long spend = 0;
        long startTime = System.currentTimeMillis();
        long endTime = startTime;
        long timeout = config.getBorrowTimeoutMs();

        pendingThreads.put(Thread.currentThread(), startTime);

        while (spend <= timeout) {
            if (closed) {
                throw new RuntimeException("adcp pool is already close");
            }

            Node borrowed = pool.borrow();

            if (borrowed != null) {
                ProxyConnection proxy = borrowed.item;
                Connection connection = proxy.rawConnection();

                boolean isValid;

                try {
                    isValid = config.getAliveCheckPeriodMs() <= 0
                            || proxy.getIdleMs() < config.getAliveCheckPeriodMs()
                            || connection.isValid(config.getCheckValidationTimeoutSeconds());
                } catch (Throwable e) {
                    logger.error("check jdbc connection error: {}", proxy, e);
                    isValid = false;
                }

                if (isValid) {
                    pendingThreads.remove(Thread.currentThread());
                    endTime = System.currentTimeMillis();
                    spend = endTime - startTime;
                    monitor.recordConnectionAcquiredMs(spend);
                    return proxy;
                } else {
                    pool.destroyActive(borrowed);
                    closeJdbcConnection(connection);
                    monitor.recordInvalidConnection();

                    logger.warn("jdbc connection {} is invalid, close it", proxy);
                }
            }

            if (pool.tryAddConnection(
                    timeout - spend,
                    waitNanos -> monitor.recordWait(waitNanos / 1000))) {
                try {
                    // if timeout, just throw SocketTimeoutException, don't retry
                    ProxyConnection proxy = createFromRawConnection();
                    endTime = System.currentTimeMillis();
                    spend = endTime - startTime;
                    monitor.recordConnectionAcquiredMs(spend);
                    return proxy;
                } catch (Throwable e) {
                    pool.failedAddConnection();
                    monitor.recordConnectionCreateError();

                    logger.error(
                            "create raw jdbc connection exception: {}, idle: {}, active: {}\nmetrics\n{}",
                            e.getMessage(),
                            pool.idleCount(),
                            pool.activeCount(),
                            monitor);

                    throw e;
                }
            }

            endTime = System.currentTimeMillis();
            spend = endTime - startTime;
        }

        monitor.recordConnectionTimeout();
        pendingThreads.remove(Thread.currentThread());
        throw new SQLTransientConnectionException(createTimeoutError(endTime, spend));
    }

    private String createTimeoutError(long endTime, long spendMs) {
        StringBuilder builder = new StringBuilder("try get connection from pool timeout: spend ")
                .append(spendMs).append(" ms > timeout:").append(config.getBorrowTimeoutMs())
                .append(", active: ").append(pool.activeCount())
                .append(", idle: ").append(pool.idleCount())
                .append("\nmetrics\n").append(monitor)
                .append("\nusing connections\n");
//
//        List<ProxyConnection> activeConnections = pool.listActiveItems();
//        activeConnections.forEach(connection -> {
//            builder.append("thread ").append(connection.getUsingThread().getName())
//                    .append(" used connection ").append(connection)
//                    .append(" for ").append(endTime - connection.getStartUsingTime()).append(" ms\n");
//        });

        if (!pendingThreads.isEmpty()) {
            builder.append("\npending threads\n");

            pendingThreads.forEach((thread, time) -> builder
                    .append(thread.getName())
                    .append(" wait ")
                    .append(endTime - time)
                    .append(" ms\n"));
        }

        return builder.toString();
    }

    private ProxyConnection createFromRawConnection() throws SQLException {
        long start = System.currentTimeMillis();
        Connection connection = dataSource.getConnection();
        long spend = System.currentTimeMillis() - start;

        monitor.recordConnectionCreatedMs(spend);

        ProxyConnection proxyConnection = new ProxyConnection(
                connectionId.incrementAndGet(),
                connection,
                this::activeToIdle);

        pool.addNewConnection(proxyConnection);

        if (spend > 5000) {
            logger.warn("created new jdbc connection: {} spend {}", proxyConnection, spend);
        } else {
            logger.info("created new jdbc connection: {} spend {}", proxyConnection, spend);
        }

        return proxyConnection;
    }

    private static void closeJdbcConnection(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error("close jdbc connection error: {}", connection, e);
        }
    }

    private void activeToIdle(ProxyConnection proxyConnection) {
        pool.giveBack();

        long usedTime = proxyConnection.getUsedTime();
        monitor.recordConnectionUsageMs(usedTime);

        if (usedTime > config.getLeakDetectionThresholdSeconds() * 1000L) {
            logger.warn("connection {} used for {} seconds", proxyConnection, usedTime / 1000);
        }
    }

    public AdcpPoolConfig getConfig() {
        return config;
    }

    public DataSource getUnwrappedDataSource() {
        return this.dataSource;
    }

    @Override
    public int getIdleConnectionCount() {
        return this.pool.idleCount();
    }

    @Override
    public int getActiveConnectionCount() {
        return pool.activeCount();
    }

    @Override
    public int getTotalConnectionCount() {
        return pool.idleCount() + pool.activeCount();
    }

    @Override
    public int getPendingThreadCount() {
        return pendingThreads.size();
    }

    @Override
    public int getMaxConnectionCount() {
        return config.getMaxPoolSize();
    }

    @Override
    public int getMinConnectionCount() {
        return config.getMinIdle();
    }

    @Override
    public String toString() {
        return "AdcpPool: " + config.getPoolName();
    }
}
