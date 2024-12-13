package com.fishqq.adcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

public class AdcpStaticFactory {
    private static final Logger logger = LoggerFactory.getLogger(AdcpStaticFactory.class);

    public static AdcpDataSource createDataSource(String jdbcUrl, AdcpDataSourceConfig config) throws SQLException {
        return createDataSource(jdbcUrl, config, new AdcpMemoryMonitor());
    }

    public static AdcpDataSource createDataSource(String jdbcUrl,
                                                  AdcpDataSourceConfig config,
                                                  AdcpMonitor monitor) throws SQLException {
        Driver driver = DriverManager.getDriver(jdbcUrl);
        return createDataSource(jdbcUrl, driver, config, monitor);
    }

    public static AdcpDataSource createDataSource(String jdbcUrl,
                                                  String driverClassName,
                                                  AdcpDataSourceConfig config) throws SQLException {
        return createDataSource(jdbcUrl, driverClassName, config, new AdcpMemoryMonitor());
    }

    public static AdcpDataSource createDataSource(String jdbcUrl,
                                                  String driverClassName,
                                                  AdcpDataSourceConfig config,
                                                  AdcpMonitor monitor) throws SQLException {
        Driver driver = getDriverByClassName(driverClassName);
        if (!driver.acceptsURL(jdbcUrl)) {
            throw new SQLException("driver: " + driverClassName + " do not accept jdbc url: " + jdbcUrl);
        }

        return createDataSource(jdbcUrl, driver, config, monitor);
    }

    public static AdcpDataSource createDataSource(String jdbcUrl, Driver driver, AdcpDataSourceConfig config) {
        return createDataSource(jdbcUrl, driver, config, new AdcpMemoryMonitor());
    }

    public static AdcpDataSource createDataSource(String jdbcUrl,
                                                  Driver driver,
                                                  AdcpDataSourceConfig config,
                                                  AdcpMonitor monitor) {
        DriverDataSource driverDataSource = new DriverDataSource(driver, jdbcUrl, config.getDriverProperties());
        return new AdcpDataSource(config, driverDataSource, monitor);
    }

    public static Driver getDriverByClassName(String clsName) throws SQLException {
        Enumeration<Driver> drivers = DriverManager.getDrivers();

        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            if (d.getClass().getName().equals(clsName)) {
                return d;
            }
        }

        logger.warn("driver class: {} was not found in driver manager", clsName);

        Class<?> cls = null;
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if (loader != null) {
            try {
                cls = loader.loadClass(clsName);
                logger.info("driver class: {} was found in thread context class loader {}", clsName, loader);
            } catch (ClassNotFoundException e) {
                logger.info("driver class: {} was not found in thread context class loader {}", clsName, loader);
            }
        }

        if (cls == null) {
            try {
                loader = AdcpPoolConfig.class.getClassLoader();
                cls = loader.loadClass(clsName);
                logger.info("driver class: {} was found in AdcpPoolConfig classloader {}", clsName, loader);
            } catch (ClassNotFoundException e) {
                logger.error("driver class: {} was not found in AdcpPoolConfig classloader {}", clsName, loader);
            }
        }

        if (cls == null) {
            throw new SQLException("can't find driver class: " + clsName);
        }

        try {
            return (Driver) cls.newInstance();
        } catch (Throwable e) {
            logger.error("failed to create instance of driver class: {}", clsName, e);
            throw new SQLException("failed to create instance of driver class: " + clsName, e);
        }
    }
}
