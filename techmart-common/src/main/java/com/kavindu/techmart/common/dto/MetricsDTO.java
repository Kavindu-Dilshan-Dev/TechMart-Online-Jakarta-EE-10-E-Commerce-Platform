package com.kavindu.techmart.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class MetricsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime timestamp = LocalDateTime.now();

    private LocalDateTime startupTime;
    private long uptimeMillis;
    private String uptimeFormatted;
    private int activeUsers;
    private int maxConcurrentUsers;
    private long totalRequests;
    private long totalOrdersProcessed;
    private long totalOrdersFailed;
    private double averageResponseTimeMs;
    private double requestsPerSecond;
    private boolean healthy = true;

    private long heapUsedBytes;
    private long heapMaxBytes;
    private long heapUsedMb;
    private long heapMaxMb;
    private double heapUsagePercent;
    private long nonHeapUsedMb;

    private int threadCount;
    private int peakThreadCount;
    private int daemonThreadCount;

    private long gcCollectionCount;
    private long gcCollectionTimeMs;

    private int availableProcessors;
    private double systemLoadAverage;
    private String jvmName;
    private String jvmVersion;

    private Map<String, Double> endpointAverageMs = new LinkedHashMap<>();
    private Map<String, Long> endpointCounts = new LinkedHashMap<>();
    private Map<String, String> circuitBreakerStates = new LinkedHashMap<>();

    public MetricsDTO() {
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getStartupTime() {
        return startupTime;
    }

    public void setStartupTime(LocalDateTime startupTime) {
        this.startupTime = startupTime;
    }

    public long getUptimeMillis() {
        return uptimeMillis;
    }

    public void setUptimeMillis(long uptimeMillis) {
        this.uptimeMillis = uptimeMillis;
    }

    public String getUptimeFormatted() {
        return uptimeFormatted;
    }

    public void setUptimeFormatted(String uptimeFormatted) {
        this.uptimeFormatted = uptimeFormatted;
    }

    public int getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(int activeUsers) {
        this.activeUsers = activeUsers;
    }

    public int getMaxConcurrentUsers() {
        return maxConcurrentUsers;
    }

    public void setMaxConcurrentUsers(int maxConcurrentUsers) {
        this.maxConcurrentUsers = maxConcurrentUsers;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getTotalOrdersProcessed() {
        return totalOrdersProcessed;
    }

    public void setTotalOrdersProcessed(long totalOrdersProcessed) {
        this.totalOrdersProcessed = totalOrdersProcessed;
    }

    public long getTotalOrdersFailed() {
        return totalOrdersFailed;
    }

    public void setTotalOrdersFailed(long totalOrdersFailed) {
        this.totalOrdersFailed = totalOrdersFailed;
    }

    public double getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public void setAverageResponseTimeMs(double averageResponseTimeMs) {
        this.averageResponseTimeMs = averageResponseTimeMs;
    }

    public double getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(double requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public long getHeapUsedBytes() {
        return heapUsedBytes;
    }

    public void setHeapUsedBytes(long heapUsedBytes) {
        this.heapUsedBytes = heapUsedBytes;
    }

    public long getHeapMaxBytes() {
        return heapMaxBytes;
    }

    public void setHeapMaxBytes(long heapMaxBytes) {
        this.heapMaxBytes = heapMaxBytes;
    }

    public long getHeapUsedMb() {
        return heapUsedMb;
    }

    public void setHeapUsedMb(long heapUsedMb) {
        this.heapUsedMb = heapUsedMb;
    }

    public long getHeapMaxMb() {
        return heapMaxMb;
    }

    public void setHeapMaxMb(long heapMaxMb) {
        this.heapMaxMb = heapMaxMb;
    }

    public double getHeapUsagePercent() {
        return heapUsagePercent;
    }

    public void setHeapUsagePercent(double heapUsagePercent) {
        this.heapUsagePercent = heapUsagePercent;
    }

    public long getNonHeapUsedMb() {
        return nonHeapUsedMb;
    }

    public void setNonHeapUsedMb(long nonHeapUsedMb) {
        this.nonHeapUsedMb = nonHeapUsedMb;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getPeakThreadCount() {
        return peakThreadCount;
    }

    public void setPeakThreadCount(int peakThreadCount) {
        this.peakThreadCount = peakThreadCount;
    }

    public int getDaemonThreadCount() {
        return daemonThreadCount;
    }

    public void setDaemonThreadCount(int daemonThreadCount) {
        this.daemonThreadCount = daemonThreadCount;
    }

    public long getGcCollectionCount() {
        return gcCollectionCount;
    }

    public void setGcCollectionCount(long gcCollectionCount) {
        this.gcCollectionCount = gcCollectionCount;
    }

    public long getGcCollectionTimeMs() {
        return gcCollectionTimeMs;
    }

    public void setGcCollectionTimeMs(long gcCollectionTimeMs) {
        this.gcCollectionTimeMs = gcCollectionTimeMs;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public double getSystemLoadAverage() {
        return systemLoadAverage;
    }

    public void setSystemLoadAverage(double systemLoadAverage) {
        this.systemLoadAverage = systemLoadAverage;
    }

    public String getJvmName() {
        return jvmName;
    }

    public void setJvmName(String jvmName) {
        this.jvmName = jvmName;
    }

    public String getJvmVersion() {
        return jvmVersion;
    }

    public void setJvmVersion(String jvmVersion) {
        this.jvmVersion = jvmVersion;
    }

    public Map<String, Double> getEndpointAverageMs() {
        return endpointAverageMs;
    }

    public void setEndpointAverageMs(Map<String, Double> endpointAverageMs) {
        this.endpointAverageMs = endpointAverageMs;
    }

    public Map<String, Long> getEndpointCounts() {
        return endpointCounts;
    }

    public void setEndpointCounts(Map<String, Long> endpointCounts) {
        this.endpointCounts = endpointCounts;
    }

    public Map<String, String> getCircuitBreakerStates() {
        return circuitBreakerStates;
    }

    public void setCircuitBreakerStates(Map<String, String> circuitBreakerStates) {
        this.circuitBreakerStates = circuitBreakerStates;
    }
}
