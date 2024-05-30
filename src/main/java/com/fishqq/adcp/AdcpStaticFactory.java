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
        Driver driver = DriverManager.getDriver(jdbcUrl);
        return createDataSource(jdbcUrl, driver, config);
    }

    public static AdcpDataSource createDataSource(String jdbcUrl,
                                                  String driverClassName,
                                                  AdcpDataSourceConfig config) throws SQLException {
        Driver driver = getDriverByClassName(driverClassName);
        if (!driver.acceptsURL(jdbcUrl)) {
            throw new RuntimeException("Driver " + driverClassName + " claims to not accept jdbcUrl, " + jdbcUrl);
        }

        return createDataSource(jdbcUrl, driver, config);
    }

    public static AdcpDataSource createDataSource(String jdbcUrl, Driver driver, AdcpDataSourceConfig config) {
        DriverDataSource driverDataSource = new DriverDataSource(driver, jdbcUrl, config.getDriverProperties());
        return new AdcpDataSource(config, driverDataSource);
    }

    public static Driver getDriverByClassName(String driverClassName) throws SQLException {
        Enumeration<Driver> drivers = DriverManager.getDrivers();

        while (drivers.hasMoreElements()) {
            Driver d = drivers.nextElement();
            if (d.getClass().getName().equals(driverClassName)) {
                return d;
            }
        }

        logger.warn(
                "Registered driver with driverClassName={} was not found, trying direct instantiation.",
                driverClassName);

        Class<?> driverClass = null;
        ClassLoader threadContextClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            if (threadContextClassLoader != null) {
                try {
                    driverClass = threadContextClassLoader.loadClass(driverClassName);
                    logger.info(
                            "Driver class {} found in Thread context class loader {}",
                            driverClassName,
                            threadContextClassLoader);
                } catch (ClassNotFoundException var12) {
                    logger.info(
                            "Driver class {} not found in Thread context class loader {}, trying classloader {}",
                            driverClassName,
                            threadContextClassLoader, AdcpStaticFactory.class.getClassLoader());
                }
            }

            if (driverClass == null) {
                driverClass = AdcpStaticFactory.class.getClassLoader().loadClass(driverClassName);
                logger.info(
                        "Driver class {} found in the HikariConfig class classloader {}",
                        driverClassName,
                        AdcpStaticFactory.class.getClassLoader());
            }
        } catch (ClassNotFoundException var13) {
            logger.error(
                    "Failed to load driver class {} from HikariConfig class classloader {}",
                    driverClassName,
                    AdcpStaticFactory.class.getClassLoader());
        }

        try {
            return (Driver) driverClass.newInstance();
        } catch (Exception e) {
            logger.error(
                    "Failed to create instance of driver class {}, trying jdbcUrl resolution",
                    driverClassName,
                    e);

            throw new SQLException("failed to create instance of driver class: " + driverClassName, e);
        }
    }
}
