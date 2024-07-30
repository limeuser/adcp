package com.fishqq.adcp.test;

import com.fishqq.adcp.AdcpDataSource;
import com.fishqq.adcp.AdcpDataSourceConfig;
import com.fishqq.adcp.AdcpStaticFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.PGProperty;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Benchmark {
    public static void main(String[] arsg) throws Exception {
        threads();
    }

    public static void threads() throws Exception {
        DataSource ds = createDataSource();

        int size = 120;
        List<Thread> threads = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Thread thread = new Thread(() -> query(ds), "biz-" + i);
            thread.setDaemon(true);
            Thread.sleep(new Random().nextInt(1000));
            thread.start();
            threads.add(thread);
        }

        Thread.sleep(10000000000L);
    }

    private static void query(DataSource ds) {
        try {
            int count = 0;

            while (true) {
                try (Connection connection = ds.getConnection()) {
                    try (PreparedStatement s = connection.prepareStatement("select 1")) {
                        s.executeQuery();
                        Thread.sleep(new Random().nextInt(50));
                    }
                } catch (Exception e) {
                    System.out.println("connect error: " + e.getMessage());
                }
                count++;

                Thread.sleep(new Random().nextInt(50));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static AdcpDataSource createDataSource() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/mc_dev";
        String username = "modeling";
        String password = "modeling";

        // config jdbc
        AdcpDataSourceConfig config = new AdcpDataSourceConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        config.setPoolName("adcp");
        config.setMinIdle(2);
        config.setMaxPoolSize(50);
        config.setLogMetrics(true);
        config.setLogWarning(true);
        config.setLogMetricsPeriodSeconds(60);
        config.getWarningConfig().setAvgWaitTimeMs(10);

        // set driver properties
        config.setDriverProperty(PGProperty.APPLICATION_NAME.getName(), "my-app");
        config.setDriverProperty(PGProperty.ADAPTIVE_FETCH_MAXIMUM.getName(), 100);

        return AdcpStaticFactory.createDataSource(config);
    }

    public static DataSource createHikari() {
        HikariConfig config = new HikariConfig();
        config.setUsername("modeling");
        config.setPassword("modeling");
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/mc_dev");

        config.setPoolName("hikari");
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(100);

        return new HikariDataSource(config);
    }
}
