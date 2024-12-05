package com.fishqq.adcp.metric.prometheus;

import com.fishqq.adcp.AdcpMetrics;
import com.fishqq.adcp.AdcpMonitor;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Summary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PrometheusMetricsTracker implements AdcpMonitor {
    private final static Counter CONNECTION_TIMEOUT_COUNTER = Counter.build()
            .name("adcp_connection_timeout_total")
            .labelNames("pool")
            .help("Connection timeout total count")
            .create();

    private final static Summary ELAPSED_ACQUIRED_SUMMARY =
            createSummary("adcp_connection_acquired_nanos", "Connection acquired time (ns)");

    private final static Summary ELAPSED_USAGE_SUMMARY =
            createSummary("adcp_connection_usage_millis", "Connection usage (ms)");

    private final static Summary ELAPSED_CREATION_SUMMARY =
            createSummary("adcp_connection_creation_millis", "Connection creation (ms)");

    private final static Map<CollectorRegistry, Boolean> registrationStatuses = new ConcurrentHashMap<>();

    private final String poolName;
    private final HikariCPCollector hikariCPCollector;

    private final Counter.Child connectionTimeoutCounterChild;

    private final Summary.Child elapsedAcquiredSummaryChild;
    private final Summary.Child elapsedUsageSummaryChild;
    private final Summary.Child elapsedCreationSummaryChild;

    PrometheusMetricsTracker(String poolName, CollectorRegistry collectorRegistry, HikariCPCollector hikariCPCollector) {
        registerMetrics(collectorRegistry);
        this.poolName = poolName;
        this.hikariCPCollector = hikariCPCollector;
        this.connectionTimeoutCounterChild = CONNECTION_TIMEOUT_COUNTER.labels(poolName);
        this.elapsedAcquiredSummaryChild = ELAPSED_ACQUIRED_SUMMARY.labels(poolName);
        this.elapsedUsageSummaryChild = ELAPSED_USAGE_SUMMARY.labels(poolName);
        this.elapsedCreationSummaryChild = ELAPSED_CREATION_SUMMARY.labels(poolName);
    }

    private void registerMetrics(CollectorRegistry collectorRegistry) {
        if (registrationStatuses.putIfAbsent(collectorRegistry, true) == null) {
            CONNECTION_TIMEOUT_COUNTER.register(collectorRegistry);
            ELAPSED_ACQUIRED_SUMMARY.register(collectorRegistry);
            ELAPSED_USAGE_SUMMARY.register(collectorRegistry);
            ELAPSED_CREATION_SUMMARY.register(collectorRegistry);
        }
    }

    @Override
    public void recordConnectionAcquiredMs(long elapsedAcquiredNanos) {
        elapsedAcquiredSummaryChild.observe(elapsedAcquiredNanos);
    }

    @Override
    public void recordConnectionUsageMs(long elapsedBorrowedMillis) {
        elapsedUsageSummaryChild.observe(elapsedBorrowedMillis);
    }

    @Override
    public void recordConnectionCreatedMs(long connectionCreatedMillis) {
        elapsedCreationSummaryChild.observe(connectionCreatedMillis);
    }

    @Override
    public void initMetrics(AdcpMetrics metrics) {

    }

    @Override
    public void recordConnectionCreateError() {

    }

    @Override
    public void recordInvalidConnection() {

    }

    @Override
    public void recordConnectionTimeout() {
        connectionTimeoutCounterChild.inc();
    }

    @Override
    public void recordWait(long ms) {

    }

    @Override
    public void reset() {

    }

    private static Summary createSummary(String name, String help) {
        return Summary.build()
                .name(name)
                .labelNames("pool")
                .help(help)
                .quantile(0.5, 0.05)
                .quantile(0.95, 0.01)
                .quantile(0.99, 0.001)
                .maxAgeSeconds(TimeUnit.MINUTES.toSeconds(5))
                .ageBuckets(5)
                .create();
    }

    @Override
    public void close() {
        hikariCPCollector.remove(poolName);
        CONNECTION_TIMEOUT_COUNTER.remove(poolName);
        ELAPSED_ACQUIRED_SUMMARY.remove(poolName);
        ELAPSED_USAGE_SUMMARY.remove(poolName);
        ELAPSED_CREATION_SUMMARY.remove(poolName);
    }
}
