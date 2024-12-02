package com.fishqq.adcp.test;

import com.fishqq.adcp.AdcpDataSource;
import com.fishqq.adcp.AdcpDataSourceConfig;
import com.fishqq.adcp.AdcpPoolConfig;
import com.fishqq.adcp.AdcpStaticFactory;
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
        config.setUsername(username);
        config.setPassword(password);

        // config pool
        config.setMaxPoolSize(10);
        config.setMinIdle(2);

        // set driver properties
        config.put(PGProperty.APPLICATION_NAME.getName(), "my-app");
        config.put(PGProperty.ADAPTIVE_FETCH_MAXIMUM.getName(), 100);

        return AdcpStaticFactory.createDataSource(url, config);
    }

    private static AdcpDataSource createByDataSource() throws SQLException {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUser("modeling");
        dataSource.setPassword("modeling");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/mc_dev");
        dataSource.setDatabaseName("mc_dev");

        dataSource.setApplicationName("my-app");

        AdcpPoolConfig poolConfig = new AdcpPoolConfig();
        poolConfig.setPoolName("adcp");
        poolConfig.setMinIdle(2);

        return new AdcpDataSource(poolConfig, dataSource);
    }
}
