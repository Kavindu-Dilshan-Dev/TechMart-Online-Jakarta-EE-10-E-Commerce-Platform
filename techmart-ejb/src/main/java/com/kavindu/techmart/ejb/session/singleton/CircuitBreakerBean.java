package com.kavindu.techmart.ejb.session.singleton;

import com.kavindu.techmart.common.enums.CircuitBreakerState;
import com.kavindu.techmart.common.exception.CircuitOpenException;
import com.kavindu.techmart.common.exception.TechMartException;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.Singleton;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Singleton(name = "CircuitBreakerBean")
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class CircuitBreakerBean {

    private static final Logger LOG = Logger.getLogger(CircuitBreakerBean.class.getName());

    private volatile int failureThreshold = 5;
    private volatile long openTimeoutMs = 30_000L;

    private final Map<String, CircuitBreakerState> states = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> failureCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastFailureTime = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public <T> T callWithBreaker(String service, Callable<T> op) {
        if (!allowRequest(service)) {
            throw new CircuitOpenException(service,
                    "Service '" + service + "' is temporarily unavailable (circuit OPEN)");
        }
        try {
            T result = op.call();
            onSuccess(service);
            return result;
        } catch (CircuitOpenException e) {
            throw e;
        } catch (Exception e) {
            onFailure(service);
            throw new TechMartException("Call to '" + service + "' failed: " + e.getMessage(), e);
        }
    }

    public boolean allowRequest(String service) {
        CircuitBreakerState state = states.getOrDefault(service, CircuitBreakerState.CLOSED);
        if (state != CircuitBreakerState.OPEN) {
            return true;
        }
        synchronized (lockFor(service)) {

            if (states.getOrDefault(service, CircuitBreakerState.CLOSED) == CircuitBreakerState.OPEN) {
                long since = now() - lastFailureTime.getOrDefault(service, 0L);
                if (since >= openTimeoutMs) {
                    states.put(service, CircuitBreakerState.HALF_OPEN);
                    LOG.info("Circuit '" + service + "' OPEN -> HALF_OPEN (probing)");
                    return true;
                }
                return false;
            }
            return true;
        }
    }

    public void onSuccess(String service) {
        synchronized (lockFor(service)) {
            CircuitBreakerState prev = states.getOrDefault(service, CircuitBreakerState.CLOSED);
            counterFor(service).set(0);
            states.put(service, CircuitBreakerState.CLOSED);
            if (prev != CircuitBreakerState.CLOSED) {
                LOG.info("Circuit '" + service + "' " + prev + " -> CLOSED (recovered)");
            }
        }
    }

    public void onFailure(String service) {
        synchronized (lockFor(service)) {
            lastFailureTime.put(service, now());
            CircuitBreakerState state = states.getOrDefault(service, CircuitBreakerState.CLOSED);
            if (state == CircuitBreakerState.HALF_OPEN) {
                states.put(service, CircuitBreakerState.OPEN);
                LOG.warning("Circuit '" + service + "' HALF_OPEN -> OPEN (probe failed)");
                return;
            }
            int failures = counterFor(service).incrementAndGet();
            if (failures >= failureThreshold) {
                states.put(service, CircuitBreakerState.OPEN);
                LOG.warning("Circuit '" + service + "' CLOSED -> OPEN after " + failures + " failures");
            }
        }
    }

    public CircuitBreakerState getState(String service) {
        return states.getOrDefault(service, CircuitBreakerState.CLOSED);
    }

    public Map<String, CircuitBreakerState> getAllStates() {
        return new LinkedHashMap<>(states);
    }

    public Map<String, String> getAllStatesAsString() {
        Map<String, String> out = new LinkedHashMap<>();
        states.forEach((k, v) -> out.put(k, v.name()));
        return out;
    }

    public int getFailureCount(String service) {
        AtomicInteger c = failureCounts.get(service);
        return c == null ? 0 : c.get();
    }

    public void reset(String service) {
        synchronized (lockFor(service)) {
            states.put(service, CircuitBreakerState.CLOSED);
            counterFor(service).set(0);
            lastFailureTime.remove(service);
            LOG.info("Circuit '" + service + "' manually reset -> CLOSED");
        }
    }

    private AtomicInteger counterFor(String service) {
        return failureCounts.computeIfAbsent(service, k -> new AtomicInteger(0));
    }

    private Object lockFor(String service) {
        return locks.computeIfAbsent(service, k -> new Object());
    }

    protected long now() {
        return System.currentTimeMillis();
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public long getOpenTimeoutMs() {
        return openTimeoutMs;
    }

    public void setOpenTimeoutMs(long openTimeoutMs) {
        this.openTimeoutMs = openTimeoutMs;
    }
}
