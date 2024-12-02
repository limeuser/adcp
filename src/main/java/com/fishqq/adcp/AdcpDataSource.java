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
        logger.info("{} - Starting...\n{}", config.getPoolName(), config);
        this.pool = new AdcpPool(config, dataSource);
        logger.info("{} - Start completed.", config.getPoolName());
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
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        AdcpPool p = pool;
        return (p != null ? p.getUnwrappedDataSource().getLogWriter() : null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        AdcpPool p = pool;
        if (p != null) {
            p.getUnwrappedDataSource().setLogWriter(out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        AdcpPool p = pool;
        if (p != null) {
            p.getUnwrappedDataSource().setLoginTimeout(seconds);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLoginTimeout() throws SQLException {
        AdcpPool p = pool;
        return (p != null ? p.getUnwrappedDataSource().getLoginTimeout() : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
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

        AdcpPool p = pool;
        if (p != null) {
            final DataSource unwrappedDataSource = p.getUnwrappedDataSource();
            if (iface.isInstance(unwrappedDataSource)) {
                return (T) unwrappedDataSource;
            }

            if (unwrappedDataSource != null) {
                return unwrappedDataSource.unwrap(iface);
            }
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

        AdcpPool p = pool;
        if (p != null) {
            final DataSource unwrappedDataSource = p.getUnwrappedDataSource();
            if (iface.isInstance(unwrappedDataSource)) {
                return true;
            }

            if (unwrappedDataSource != null) {
                return unwrappedDataSource.isWrapperFor(iface);
            }
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
