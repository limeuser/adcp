package com.fishqq.adcp.test;

import com.fishqq.adcp.AdcpDataSource;
import com.fishqq.adcp.AdcpDataSourceConfig;
import com.fishqq.adcp.AdcpMemoryMonitor;
import com.fishqq.adcp.AdcpMonitor;
import com.fishqq.adcp.AdcpStaticFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.postgresql.PGProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public class Benchmark {
    private final static Logger logger = LoggerFactory.getLogger(Benchmark.class);

    @Test
    public void singleThread() throws SQLException {
        int count = 100000000;
        logger.info("hikari spend: {}", acquireConnection(createHikariDataSource(), count));
        logger.info("adcp spend: {}", acquireConnection(createAdcpDataSource(), count));
    }

    private long acquireConnection(DataSource ds, long count) {
        try {
            try (Connection connection = ds.getConnection()) {

            }

            long start = System.currentTimeMillis();

            for (int i = 0; i < count; i++) {
                try (Connection connection = ds.getConnection()) {

                }
            }

            long spend = System.currentTimeMillis() - start;
            logger.info("thread {} spend {}", Thread.currentThread().getName(), spend);
            return spend;
        } catch (SQLException e) {
            logger.error("error", e);
            throw new RuntimeException(e);
        }
    }

    @Test
    public void multiThreads() throws Exception {
        int threadCount = 100;
        int count = 1000000;

        logger.info("adcp spend: {}", multiThreads(threadCount, createAdcpDataSource(), count));
//        logger.info("hikari spend: {}", multiThreads(threadCount, createHikariDataSource(), count));
    }

    public long multiThreads(int threadCount, DataSource ds, int count) throws Exception {
        AtomicLong totalTime = new AtomicLong(0);

        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        List<Thread> threads = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                totalTime.addAndGet(acquireConnection(ds, count));
                countDownLatch.countDown();
            });
            thread.start();
            threads.add(thread);
        }

        countDownLatch.await();

        return totalTime.get();
    }

    private static void query(DataSource ds) {
        try {
            int max = 1000 + new Random().nextInt(100);
            int count = 0;

            while (count < max) {
                try (Connection connection = ds.getConnection()) {
                    try (PreparedStatement s = connection.prepareStatement("select 1")) {
                        s.executeQuery();
                        Thread.sleep(new Random().nextInt(10));
                    }
                } catch (Exception e) {
                    System.out.println("connection exception: " + e.getMessage());
                }

                count++;


                Thread.sleep(new Random().nextInt(30));
            }

            System.out.println("thread " + Thread.currentThread().getName() + " done");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AdcpDataSource createAdcpDataSource() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/mc_dev";
        String username = "modeling";
        String password = "modeling";

        // config jdbc
        AdcpDataSourceConfig config = new AdcpDataSourceConfig();
        config.setUsername(username);
        config.setPassword(password);

        config.setPoolName("adcp");
        config.setMinIdle(2);
        config.setMaxPoolSize(100);

        // set driver properties
        config.setDriverProperty(PGProperty.APPLICATION_NAME.getName(), "my-app");
        config.setDriverProperty(PGProperty.ADAPTIVE_FETCH_MAXIMUM.getName(), 100);

        AdcpMonitor monitor = new AdcpMemoryMonitor();
        AdcpDataSource ds = AdcpStaticFactory.createDataSource(url, config, monitor);
        monitor.initMetrics(ds.getMetrics());
        return ds;
    }

    public static DataSource createHikariDataSource() {
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
