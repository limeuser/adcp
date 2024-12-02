package com.fishqq.adcp;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Properties;

public final class DriverDataSource implements DataSource {
    private final Driver driver;
    private final String jdbcUrl;
    private final Properties driverProperties;
    private final Properties driverPropertiesWithUserPassword;

    public DriverDataSource(Driver driver, String jdbcUrl, Properties driverProperties) {
        this.driver = driver;
        this.jdbcUrl = jdbcUrl;
        this.driverProperties = driverProperties;
        this.driverPropertiesWithUserPassword = new Properties(driverProperties);
    }

    public Connection getConnection() throws SQLException {
        return driver.connect(jdbcUrl, driverProperties);
    }

    public Connection getConnection(String username, String password) throws SQLException {
        driverPropertiesWithUserPassword.put("user", username);
        driverPropertiesWithUserPassword.put("password", password);

        return this.driver.connect(jdbcUrl, driverProperties);
    }

    public PrintWriter getLogWriter() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setLogWriter(PrintWriter logWriter) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        DriverManager.setLoginTimeout(seconds);
    }

    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return this.driver.getParentLogger();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
