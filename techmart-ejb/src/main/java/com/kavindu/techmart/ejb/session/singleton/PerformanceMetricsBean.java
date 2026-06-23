package com.kavindu.techmart.ejb.session.singleton;

import com.kavindu.techmart.common.dto.MetricsDTO;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@Singleton(name = "PerformanceMetricsBean")
@Startup
@DependsOn({"SystemConfigBean", "CircuitBreakerBean"})
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class PerformanceMetricsBean {

    private static final Logger LOG = Logger.getLogger(PerformanceMetricsBean.class.getName());
    private static final int MAX_SAMPLES = 120;

    @EJB
    private SystemConfigBean systemConfig;

    @EJB
    private CircuitBreakerBean circuitBreaker;

    private final AtomicLong totalRequests = new AtomicLong();
    private final AtomicLong totalResponseTimeMs = new AtomicLong();
    private final AtomicInteger activeUsers = new AtomicInteger();
    private final AtomicInteger peakActiveUsers = new AtomicInteger();
    private final AtomicLong totalOrdersProcessed = new AtomicLong();
    private final AtomicLong totalOrdersFailed = new AtomicLong();

    private final Map<String, EndpointStat> endpointStats = new ConcurrentHashMap<>();
    private final Deque<Sample> recentSamples = new ConcurrentLinkedDeque<>();

    private long startupTimestamp;
    private LocalDateTime startupTime;

    @PostConstruct
    public void init() {
        startupTimestamp = System.currentTimeMillis();
        startupTime = LocalDateTime.now();
        LOG.info("PerformanceMetricsBean started at " + startupTime);
    }

    public void recordRequest(String endpoint, long responseTimeMs) {
        totalRequests.incrementAndGet();
        totalResponseTimeMs.addAndGet(responseTimeMs);
        endpointStats.computeIfAbsent(endpoint, k -> new EndpointStat()).record(responseTimeMs);
    }

    public void incrementActiveUsers() {
        int current = activeUsers.incrementAndGet();
        peakActiveUsers.accumulateAndGet(current, Math::max);
    }

    public void decrementActiveUsers() {
        activeUsers.updateAndGet(v -> v > 0 ? v - 1 : 0);
    }

    public void setActiveUsers(int value) {
        activeUsers.set(Math.max(0, value));
        peakActiveUsers.accumulateAndGet(value, Math::max);
    }

    public void recordOrderProcessed() {
        totalOrdersProcessed.incrementAndGet();
    }

    public void recordOrderFailed() {
        totalOrdersFailed.incrementAndGet();
    }

    public int getActiveUsers() {
        return activeUsers.get();
    }

    public long getTotalRequests() {
        return totalRequests.get();
    }

    public long getUptimeMillis() {
        return System.currentTimeMillis() - startupTimestamp;
    }

    public double getAverageResponseTimeMs() {
        long count = totalRequests.get();
        return count == 0 ? 0.0 : (double) totalResponseTimeMs.get() / count;
    }

    public MetricsDTO getMetrics() {
        return buildSnapshot();
    }

    public MetricsDTO getJvmMetrics() {
        return buildSnapshot();
    }

    private MetricsDTO buildSnapshot() {
        MetricsDTO m = new MetricsDTO();
        m.setTimestamp(LocalDateTime.now());

        long uptime = getUptimeMillis();
        m.setStartupTime(startupTime);
        m.setUptimeMillis(uptime);
        m.setUptimeFormatted(formatDuration(uptime));
        m.setActiveUsers(activeUsers.get());
        m.setMaxConcurrentUsers(systemConfig != null ? systemConfig.getMaxConcurrentUsers() : 10000);
        long requests = totalRequests.get();
        m.setTotalRequests(requests);
        m.setTotalOrdersProcessed(totalOrdersProcessed.get());
        m.setTotalOrdersFailed(totalOrdersFailed.get());
        m.setAverageResponseTimeMs(round(getAverageResponseTimeMs()));
        double seconds = uptime / 1000.0;
        m.setRequestsPerSecond(seconds > 0 ? round(requests / seconds) : 0.0);

        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();
        long heapUsed = heap.getUsed();
        long heapMax = heap.getMax();
        m.setHeapUsedBytes(heapUsed);
        m.setHeapMaxBytes(heapMax);
        m.setHeapUsedMb(toMb(heapUsed));
        m.setHeapMaxMb(toMb(heapMax));
        m.setHeapUsagePercent(heapMax > 0 ? round(100.0 * heapUsed / heapMax) : 0.0);
        m.setNonHeapUsedMb(toMb(nonHeap.getUsed()));

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        m.setThreadCount(threads.getThreadCount());
        m.setPeakThreadCount(threads.getPeakThreadCount());
        m.setDaemonThreadCount(threads.getDaemonThreadCount());

        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gc.getCollectionCount() > 0) {
                gcCount += gc.getCollectionCount();
            }
            if (gc.getCollectionTime() > 0) {
                gcTime += gc.getCollectionTime();
            }
        }
        m.setGcCollectionCount(gcCount);
        m.setGcCollectionTimeMs(gcTime);

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        m.setAvailableProcessors(os.getAvailableProcessors());
        m.setSystemLoadAverage(round(os.getSystemLoadAverage()));
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        m.setJvmName(runtime.getVmName());
        m.setJvmVersion(runtime.getVmVersion());

        Map<String, Double> avg = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();
        endpointStats.forEach((k, v) -> {
            avg.put(k, round(v.getAverage()));
            counts.put(k, v.getCount());
        });
        m.setEndpointAverageMs(avg);
        m.setEndpointCounts(counts);

        if (circuitBreaker != null) {
            m.setCircuitBreakerStates(circuitBreaker.getAllStatesAsString());
        }

        boolean healthy = !(systemConfig != null && systemConfig.isMaintenanceMode())
                && m.getHeapUsagePercent() < 95.0;
        m.setHealthy(healthy);

        return m;
    }

    public List<Map<String, Object>> getRecentSamples() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Sample s : recentSamples) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", s.timestamp);
            row.put("heapUsedMb", s.heapUsedMb);
            row.put("threadCount", s.threadCount);
            row.put("gcTimeMs", s.gcTimeMs);
            row.put("avgResponseMs", s.avgResponseMs);
            row.put("activeUsers", s.activeUsers);
            out.add(row);
        }
        return out;
    }

    @Schedule(hour = "*", minute = "*/5", persistent = false, info = "gc-metrics-sampler")
    public void collectGcMetrics() {
        MetricsDTO snapshot = buildSnapshot();
        Sample sample = new Sample();
        sample.timestamp = System.currentTimeMillis();
        sample.heapUsedMb = snapshot.getHeapUsedMb();
        sample.threadCount = snapshot.getThreadCount();
        sample.gcTimeMs = snapshot.getGcCollectionTimeMs();
        sample.avgResponseMs = snapshot.getAverageResponseTimeMs();
        sample.activeUsers = snapshot.getActiveUsers();
        recentSamples.addLast(sample);
        while (recentSamples.size() > MAX_SAMPLES) {
            recentSamples.pollFirst();
        }
        LOG.fine(() -> "GC sample: heapUsedMb=" + sample.heapUsedMb
                + " gcTimeMs=" + sample.gcTimeMs + " threads=" + sample.threadCount);
    }

    private static long toMb(long bytes) {
        return bytes <= 0 ? 0 : bytes / (1024 * 1024);
    }

    private static double round(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0;
        }
        return Math.round(v * 100.0) / 100.0;
    }

    private static String formatDuration(long millis) {
        Duration d = Duration.ofMillis(millis);
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (days > 0) {
            return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        }
        return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
    }

    private static final class EndpointStat {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong totalMs = new AtomicLong();

        void record(long ms) {
            count.incrementAndGet();
            totalMs.addAndGet(ms);
        }

        long getCount() {
            return count.get();
        }

        double getAverage() {
            long c = count.get();
            return c == 0 ? 0.0 : (double) totalMs.get() / c;
        }
    }

    private static final class Sample {
        long timestamp;
        long heapUsedMb;
        int threadCount;
        long gcTimeMs;
        double avgResponseMs;
        int activeUsers;
    }
}
