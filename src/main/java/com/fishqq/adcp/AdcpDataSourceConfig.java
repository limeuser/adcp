package com.fishqq.adcp;

import java.util.Properties;

public class AdcpDataSourceConfig extends AdcpPoolConfig {
    private String jdbcUrl;
    private String driverClassName;
    private final WarningConfig warningConfig = new WarningConfig();
    private final Properties driverProperties = new Properties();

    Properties getDriverProperties() {
        return driverProperties;
    }

    public void setDriverProperty(String key, Object value) {
        this.driverProperties.put(key, value);
    }

    public Object getDriverProperty(String key) {
        return driverProperties.get(key);
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getUsername() {
        return driverProperties.getProperty(DriverPropertyKey.user.name());
    }

    public void setUsername(String username) {
        driverProperties.setProperty(DriverPropertyKey.user.name(), username);
    }

    public String getPassword() {
        return driverProperties.getProperty(DriverPropertyKey.password.name());
    }

    public void setPassword(String password) {
        driverProperties.setProperty(DriverPropertyKey.password.name(), password);
    }

    public String getPlatform() {
        return driverProperties.getProperty(DriverPropertyKey.platform.name());
    }

    public void setPlatform(String platform) {
        driverProperties.setProperty(DriverPropertyKey.platform.name(), platform);
    }

    public boolean getContinueOnError() {
        return (boolean) driverProperties.getOrDefault(DriverPropertyKey.continueOnError.name(), false);
    }

    public void setContinueOnError(boolean continueOnError) {
        driverProperties.put(DriverPropertyKey.continueOnError.name(), continueOnError);
    }

    public WarningConfig getWarningConfig() {
        return warningConfig;
    }
}
