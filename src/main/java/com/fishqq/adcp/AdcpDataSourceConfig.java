package com.fishqq.adcp;

import java.util.Properties;

public class AdcpDataSourceConfig extends AdcpPoolConfig {
    private final Properties driverProperties = new Properties();

    Properties getDriverProperties() {
        return driverProperties;
    }

    public void put(String key, Object value) {
        this.driverProperties.put(key, value);
    }

    public Object get(String key) {
        return driverProperties.get(key);
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
}
