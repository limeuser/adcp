package com.fishqq.adcp.metric.micrometer;

import com.fishqq.adcp.AdcpMetrics;
import com.fishqq.adcp.AdcpMonitor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.TimeUnit;

public class MicrometerAdcpMonitor implements AdcpMonitor {
    private final String poolName;
    public static final String METRIC_NAME_PREFIX = "adcp";
    private static final String METRIC_CATEGORY = "pool";

    private static final String METRIC_NAME_ACQUIRE = METRIC_NAME_PREFIX + ".connections.acquire";
    private static final String METRIC_NAME_WAIT_CREATE = METRIC_NAME_PREFIX + ".connections.wait.create";
    private static final String METRIC_NAME_USAGE = METRIC_NAME_PREFIX + ".connections.usage";
    private static final String METRIC_NAME_CONNECT = METRIC_NAME_PREFIX + ".connections.creation";

    private static final String METRIC_NAME_TIMEOUT_RATE = METRIC_NAME_PREFIX + ".connections.timeout";
    private static final String METRIC_NAME_INVALID_CONNECTION_RATE = METRIC_NAME_PREFIX + ".connections.invalid";
    private static final String METRIC_NAME_CONNECTION_CREATE_ERROR_RATE = METRIC_NAME_PREFIX + ".connections.create.error";
    private static final String METRIC_NAME_CONNECTION_WAIT_RATE = METRIC_NAME_PREFIX + ".connections.wait";
    private static final String METRIC_NAME_TOTAL_CONNECTIONS = METRIC_NAME_PREFIX + ".connections";
    private static final String METRIC_NAME_IDLE_CONNECTIONS = METRIC_NAME_PREFIX + ".connections.idle";
    private static final String METRIC_NAME_ACTIVE_CONNECTIONS = METRIC_NAME_PREFIX + ".connections.active";
    private static final String METRIC_NAME_PENDING_CONNECTIONS = METRIC_NAME_PREFIX + ".connections.pending";
    private static final String METRIC_NAME_MAX_CONNECTIONS = METRIC_NAME_PREFIX + ".connections.max";
    private static final String METRIC_NAME_MIN_CONNECTIONS = METRIC_NAME_PREFIX + ".connections.min";

    private final Timer connectionAcquireTimer;
    private final Timer connectionUsageTimer;
    private final Timer connectionCreationTimer;
    private final Timer connectionWaitCreateTimer;
    private final Counter connectionTimeoutCounter;
    private final Counter invalidConnectionCounter;
    private final Counter connectionCreateErrorCounter;
    private final Counter connectionWaitCreateCounter;
    private Gauge totalConnectionGauge;
    private Gauge idleConnectionGauge;
    private Gauge activeConnectionGauge;
    private Gauge pendingConnectionGauge;
    private Gauge maxConnectionGauge;
    private Gauge minConnectionGauge;
    private final MeterRegistry meterRegistry;

    public MicrometerAdcpMonitor(String poolName, MeterRegistry meterRegistry) {
        this.poolName = poolName;
        this.meterRegistry = meterRegistry;

        this.connectionAcquireTimer = Timer.builder(METRIC_NAME_ACQUIRE)
                .description("Connection acquire time")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.connectionWaitCreateTimer = Timer.builder(METRIC_NAME_WAIT_CREATE)
                .description("Connection wait create time")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.connectionCreationTimer = Timer.builder(METRIC_NAME_CONNECT)
                .description("Connection creation time")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.connectionUsageTimer = Timer.builder(METRIC_NAME_USAGE)
                .description("Connection usage time")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.connectionTimeoutCounter = Counter.builder(METRIC_NAME_TIMEOUT_RATE)
                .description("Connection timeout total count")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.invalidConnectionCounter = Counter.builder(METRIC_NAME_INVALID_CONNECTION_RATE)
                .description("Connection invalid total count")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.connectionCreateErrorCounter = Counter.builder(METRIC_NAME_CONNECTION_CREATE_ERROR_RATE)
                .description("Connection create error count")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.connectionWaitCreateCounter = Counter.builder(METRIC_NAME_CONNECTION_WAIT_RATE)
                .description("Connection wait create count")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);
    }

    @Override
    public void initMetrics(AdcpMetrics metrics) {
        this.totalConnectionGauge = Gauge.builder(
                        METRIC_NAME_TOTAL_CONNECTIONS, metrics, AdcpMetrics::getTotalConnectionCount)
                .description("Total connections")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.idleConnectionGauge = Gauge.builder(
                        METRIC_NAME_IDLE_CONNECTIONS, metrics, AdcpMetrics::getIdleConnectionCount)
                .description("Idle connections")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.activeConnectionGauge = Gauge.builder(
                        METRIC_NAME_ACTIVE_CONNECTIONS, metrics, AdcpMetrics::getActiveConnectionCount)
                .description("Active connections")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.pendingConnectionGauge = Gauge.builder(
                        METRIC_NAME_PENDING_CONNECTIONS, metrics, AdcpMetrics::getPendingThreadCount)
                .description("Pending threads")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.maxConnectionGauge = Gauge.builder(
                        METRIC_NAME_MAX_CONNECTIONS, metrics, AdcpMetrics::getMaxConnectionCount)
                .description("Max connections")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);

        this.minConnectionGauge = Gauge.builder(
                        METRIC_NAME_MIN_CONNECTIONS, metrics, AdcpMetrics::getMinConnectionCount)
                .description("Min idle connections")
                .tags(METRIC_CATEGORY, poolName)
                .register(meterRegistry);
    }

    @Override
    public void recordConnectionAcquiredMs(long ms) {
        connectionAcquireTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordConnectionUsageMs(long ms) {
        connectionUsageTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordConnectionTimeout() {
        connectionTimeoutCounter.increment();
    }

    @Override
    public void recordWait(long ms) {
        connectionWaitCreateCounter.increment();
        connectionWaitCreateTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordInvalidConnection() {
        invalidConnectionCounter.increment();
    }

    @Override
    public void recordConnectionCreatedMs(long ms) {
        connectionCreationTimer.record(ms, TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordConnectionCreateError() {
        connectionCreateErrorCounter.increment();
    }

    @Override
    public void reset() {

    }

    @Override
    public void close() {
        meterRegistry.remove(connectionAcquireTimer);
        meterRegistry.remove(connectionWaitCreateTimer);
        meterRegistry.remove(connectionUsageTimer);
        meterRegistry.remove(connectionCreationTimer);
        meterRegistry.remove(connectionTimeoutCounter);
        meterRegistry.remove(invalidConnectionCounter);
        meterRegistry.remove(connectionCreateErrorCounter);
        meterRegistry.remove(connectionWaitCreateCounter);
        meterRegistry.remove(totalConnectionGauge);
        meterRegistry.remove(idleConnectionGauge);
        meterRegistry.remove(activeConnectionGauge);
        meterRegistry.remove(pendingConnectionGauge);
        meterRegistry.remove(maxConnectionGauge);
        meterRegistry.remove(minConnectionGauge);
    }
}
