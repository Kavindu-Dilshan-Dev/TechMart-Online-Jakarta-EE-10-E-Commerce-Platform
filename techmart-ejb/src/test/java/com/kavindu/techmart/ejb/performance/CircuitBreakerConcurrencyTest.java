package com.kavindu.techmart.ejb.performance;

import com.kavindu.techmart.common.enums.CircuitBreakerState;
import com.kavindu.techmart.ejb.session.singleton.CircuitBreakerBean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircuitBreakerConcurrencyTest {

    @Test
    @DisplayName("Stays CLOSED and lossless under 40 threads x 5,000 successful calls")
    void concurrentSuccesses() throws Exception {
        CircuitBreakerBean breaker = new CircuitBreakerBean();
        int threads = 40;
        int perThread = 5_000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();

        long t0 = System.nanoTime();
        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        String r = breaker.callWithBreaker("svc", () -> "v");
                        if ("v".equals(r)) {
                            ok.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(60, TimeUnit.SECONDS), "all workers should finish");
        pool.shutdownNow();

        long totalCalls = (long) threads * perThread;
        double seconds = (System.nanoTime() - t0) / 1_000_000_000.0;
        System.out.printf("CircuitBreaker throughput: %,d calls in %.3fs = %,.0f calls/s%n",
                totalCalls, seconds, totalCalls / seconds);

        assertEquals(totalCalls, ok.get(), "no calls should be lost");
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState("svc"));
    }

    @Test
    @DisplayName("Opens deterministically under a burst of concurrent failures")
    void concurrentFailuresOpenCircuit() throws Exception {
        CircuitBreakerBean breaker = new CircuitBreakerBean();
        breaker.setFailureThreshold(5);
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        try {
                            breaker.callWithBreaker("flaky", () -> {
                                throw new RuntimeException("boom");
                            });
                        } catch (RuntimeException ignored) {

                        }
                    }
                } finally {
                    done.countDown();
                }
            });
        }
        assertTrue(done.await(30, TimeUnit.SECONDS));
        pool.shutdownNow();
        assertEquals(CircuitBreakerState.OPEN, breaker.getState("flaky"));
    }
}
