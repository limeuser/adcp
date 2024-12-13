package com.fishqq.adcp;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class ProxyConnection implements Connection {
    private final long id;
    private final Connection connection;
    private final Consumer<ProxyConnection> closeHandler;

    private final AtomicBoolean isClosed;

    private final long createdAt;
    private long lastUsingTime;
    private long startUsingTime;

    private Boolean originAutoCommit;
    private Boolean originReadOnly;
    private Integer originIsolationLevel;
    private String originCatalog;
    private String originSchema;

    public ProxyConnection(long id, Connection connection, Consumer<ProxyConnection> closeHandler) {
        this.id = id;
        this.connection = connection;
        this.closeHandler = closeHandler;
        this.createdAt = System.currentTimeMillis();
        this.lastUsingTime = this.createdAt;
        this.startUsingTime = this.createdAt;
        this.isClosed = new AtomicBoolean(false);
    }

    public Connection getRawConnection() {
        return this.connection;
    }

    public boolean tryUse() {
        boolean r = this.isClosed.compareAndSet(true, false);
        if (r) {
            this.startUsingTime = System.currentTimeMillis();
        }
        return r;
    }

    @Override
    public final void close() throws SQLException {
        this.closeHandler.accept(this);

        this.isClosed.set(true);
        this.lastUsingTime = System.currentTimeMillis();

        this.resetStatus();
    }

    public long getUsedTime() {
        return System.currentTimeMillis() - startUsingTime;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    void resetStatus() throws SQLException {
        if (this.originAutoCommit != null) {
            this.connection.setAutoCommit(originAutoCommit);
        }
        if (this.originReadOnly != null) {
            this.connection.setReadOnly(originReadOnly);
        }
        if (this.originIsolationLevel != null) {
            this.connection.setTransactionIsolation(originIsolationLevel);
        }
        if (this.originCatalog != null) {
            this.connection.setCatalog(originCatalog);
        }
        if (this.originSchema != null) {
            this.connection.setSchema(originSchema);
        }
    }

    public Connection rawConnection() {
        return this.connection;
    }

    public long getIdleMs() {
        return System.currentTimeMillis() - this.lastUsingTime;
    }

    @Override
    public boolean isClosed() {
        return this.isClosed.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statement createStatement() throws SQLException {
        return this.connection.createStatement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statement createStatement(int resultSetType, int concurrency) throws SQLException {
        return this.connection.createStatement(resultSetType, concurrency);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Statement createStatement(int resultSetType, int concurrency, int horiginability) throws SQLException {
        return this.connection.createStatement(resultSetType, concurrency, horiginability);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        return this.connection.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        return this.connection.nativeSQL(sql);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int concurrency) throws SQLException {
        return this.connection.prepareCall(sql, resultSetType, concurrency);
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return this.connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        this.connection.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        this.connection.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
        return this.connection.getHoldability();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return this.connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return this.connection.setSavepoint(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int concurrency, int horiginability) throws SQLException {
        return this.connection.prepareCall(sql, resultSetType, concurrency, horiginability);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return this.connection.prepareStatement(sql);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return this.connection.prepareStatement(sql, autoGeneratedKeys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int concurrency) throws SQLException {
        return this.connection.prepareStatement(sql, resultSetType, concurrency);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int concurrency, int horiginability) throws SQLException {
        return this.connection.prepareStatement(sql, resultSetType, concurrency, horiginability);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return this.connection.prepareStatement(sql, columnIndexes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return this.connection.prepareStatement(sql, columnNames);
    }

    @Override
    public Clob createClob() throws SQLException {
        return this.connection.createClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        return this.connection.createBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return this.connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return this.connection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return this.connection.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        this.connection.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        this.connection.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        return this.connection.getClientInfo(name);
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return this.connection.getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return this.connection.createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return this.connection.createStruct(typeName, attributes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return this.connection.getMetaData();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit() throws SQLException {
        this.connection.commit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback() throws SQLException {
        this.connection.rollback();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        this.connection.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        this.connection.releaseSavepoint(savepoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (this.originAutoCommit == null) {
            boolean tempAutoCommit = this.connection.getAutoCommit();
            if (tempAutoCommit != autoCommit) {
                this.connection.setAutoCommit(autoCommit);
                this.originAutoCommit = autoCommit;
            }
        } else {
            this.connection.setAutoCommit(autoCommit);
        }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return this.connection.getAutoCommit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        if (this.originReadOnly == null) {
            boolean tempReadOnly = this.connection.isReadOnly();
            if (tempReadOnly != readOnly) {
                this.connection.setReadOnly(readOnly);
                this.originReadOnly = tempReadOnly;
            }
        } else {
            this.connection.setReadOnly(readOnly);
        }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return this.connection.isReadOnly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (this.originIsolationLevel == null) {
            int tempIsolationLevel = this.connection.getTransactionIsolation();
            if (tempIsolationLevel != level) {
                this.connection.setTransactionIsolation(level);
                this.originIsolationLevel = level;
            }
        } else {
            this.connection.setTransactionIsolation(level);
        }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return this.connection.getTransactionIsolation();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return this.connection.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        this.connection.clearWarnings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCatalog(String catalog) throws SQLException {
        if (this.originCatalog == null) {
            String tempCatalog = this.connection.getCatalog();
            if (catalog.equals(tempCatalog)) {
                this.connection.setCatalog(catalog);
                this.originCatalog = tempCatalog;
            }
        } else {
            this.connection.setCatalog(catalog);
        }
    }

    @Override
    public String getCatalog() throws SQLException {
        return this.connection.getCatalog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        this.connection.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return this.connection.getNetworkTimeout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSchema(String schema) throws SQLException {
        if (this.originSchema == null) {
            String tempSchema = this.connection.getSchema();
            if (schema.equals(tempSchema)) {
                this.connection.setSchema(schema);
                this.originSchema = schema;
            }
        } else {
            this.connection.setSchema(schema);
        }
    }

    @Override
    public String getSchema() throws SQLException {
        return this.connection.getSchema();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        this.connection.abort(executor);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.connection.isWrapperFor(iface);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T unwrap(Class<T> iface) throws SQLException {
        return this.connection.unwrap(iface);
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }
}