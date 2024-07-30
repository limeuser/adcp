package com.fishqq.adcp.test;

import com.fishqq.adcp.AdcpDataSource;
import com.fishqq.adcp.AdcpDataSourceConfig;
import com.fishqq.adcp.AdcpPoolConfig;
import com.fishqq.adcp.AdcpStaticFactory;
import com.fishqq.adcp.WarningConfig;
import org.postgresql.PGProperty;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Example {
    public static void main(String[] args) throws SQLException {
        AdcpDataSource ds = createByDataSource();
        try (Connection connection = ds.getConnection();
             PreparedStatement s = connection.prepareStatement("select table_name from information_schema.tables");
             ResultSet rs = s.executeQuery()) {
            while (rs.next()) {
                String t = rs.getString(1);
                System.out.println(t);
            }
        }

        ds.close();
    }

    public static AdcpDataSource createByJdbcUrl() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/mc_dev";
        String username = "modeling";
        String password = "modeling";

        // config jdbc
        AdcpDataSourceConfig config = new AdcpDataSourceConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        // config pool
        config.setMaxPoolSize(10);
        config.setMinIdle(2);
        config.setLogWarning(true);
        config.setLogMetricsPeriodSeconds(120);

        // metrics warning config
        config.getWarningConfig().setAvgWaitTimeMs(1000);

        // set driver properties
        config.setDriverProperty(PGProperty.APPLICATION_NAME.getName(), "my-app");
        config.setDriverProperty(PGProperty.ADAPTIVE_FETCH_MAXIMUM.getName(), 100);

        return AdcpStaticFactory.createDataSource(config);
    }

    private static AdcpDataSource createByDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUser("modeling");
        dataSource.setPassword("modeling");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/mc_dev");
        dataSource.setDatabaseName("mc_dev");

        dataSource.setApplicationName("my-app");

        AdcpPoolConfig config = new AdcpPoolConfig();

        config.setPoolName("adcp");
        config.setMinIdle(2);
        config.setMaxPoolSize(10);

        // metrics warning config
        config.setLogWarning(true);
        config.setLogMetricsPeriodSeconds(120);

        return new AdcpDataSource(config, new WarningConfig(), dataSource);
    }
}
