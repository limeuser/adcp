package com.fishqq.adcp.metric.prometheus;

import com.fishqq.adcp.AdcpMetrics;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

class HikariCPCollector extends Collector {
    private static final List<String> LABEL_NAMES = Collections.singletonList("pool");

    private final Map<String, AdcpMetrics> metricsMap = new ConcurrentHashMap<>();

    @Override
    public List<MetricFamilySamples> collect() {
        return Arrays.asList(
                createGauge("adcp_active_connections", "Active connections",
                        AdcpMetrics::getActiveConnectionCount),
                createGauge("adcp_idle_connections", "Idle connections",
                        AdcpMetrics::getIdleConnectionCount),
                createGauge("adcp_pending_threads", "Pending threads",
                        AdcpMetrics::getPendingThreadCount),
                createGauge("adcp_connections", "The number of current connections",
                        AdcpMetrics::getTotalConnectionCount),
                createGauge("adcp_max_connections", "Max connections",
                        AdcpMetrics::getMaxConnectionCount),
                createGauge("adcp_min_connections", "Min connections",
                        AdcpMetrics::getMinConnectionCount)
        );
    }

    void add(String name, AdcpMetrics metrics) {
        metricsMap.put(name, metrics);
    }

    void remove(String name) {
        metricsMap.remove(name);
    }

    private GaugeMetricFamily createGauge(String metric, String help,
                                          Function<AdcpMetrics, Integer> metricValueFunction) {
        GaugeMetricFamily metricFamily = new GaugeMetricFamily(metric, help, LABEL_NAMES);
        metricsMap.forEach((k, v) -> metricFamily.addMetric(
                Collections.singletonList(k),
                metricValueFunction.apply(v)
        ));
        return metricFamily;
    }
}
