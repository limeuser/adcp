package com.fishqq.adcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

public class AdcpDataSource implements DataSource, Closeable {
    private final AdcpPool pool;
    private volatile boolean isClosed = false;

    private static final Logger logger = LoggerFactory.getLogger(AdcpDataSource.class);

    public AdcpDataSource(AdcpPoolConfig config, DataSource dataSource) {
        this(config, dataSource, new AdcpMemoryMonitor());
    }

    public AdcpDataSource(AdcpPoolConfig config, DataSource dataSource, AdcpMonitor monitor) {
        logger.info("{} - Starting...\n{}", config.getPoolName(), config);
        this.pool = new AdcpPool(config, dataSource, monitor);
        logger.info("{} - Start completed.", config.getPoolName());
    }

    public AdcpMetrics getMetrics() {
        return pool;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection getConnection() throws SQLException {
        if (isClosed) {
            throw new SQLException("AdcpDataSource" + this + " has been closed.");
        }

        return pool.borrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        throw new SQLFeatureNotSupportedException("adcp datasource don't support to get connection with username and password");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return pool.getUnwrappedDataSource().getLogWriter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        pool.getUnwrappedDataSource().setLogWriter(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        pool.getUnwrappedDataSource().setLoginTimeout(seconds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        return pool.getUnwrappedDataSource().getLoginTimeout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("adcp datasource don't support to getParentLogger");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }

        DataSource unwrappedDataSource = pool.getUnwrappedDataSource();
        if (iface.isInstance(unwrappedDataSource)) {
            return (T) unwrappedDataSource;
        }

        if (unwrappedDataSource != null) {
            return unwrappedDataSource.unwrap(iface);
        }

        throw new SQLException("Wrapped DataSource is not an instance of " + iface);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return true;
        }

        DataSource unwrappedDataSource = pool.getUnwrappedDataSource();
        if (iface.isInstance(unwrappedDataSource)) {
            return true;
        }

        if (unwrappedDataSource != null) {
            return unwrappedDataSource.isWrapperFor(iface);
        }

        return false;
    }

    /**
     * Shutdown the DataSource and its associated pool.
     */
    @Override
    public synchronized void close() {
        if (isClosed) {
            return;
        }

        try {
            isClosed = true;
            logger.info("{} - Shutdown initiated...", pool.getConfig().getPoolName());
            pool.shutdown();
            logger.info("{} - Shutdown completed.", pool.getConfig().getPoolName());
        } finally {
            isClosed = true;
        }
    }
}
