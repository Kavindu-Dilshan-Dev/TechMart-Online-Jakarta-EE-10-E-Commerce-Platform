package com.kavindu.techmart.ejb.session.singleton;

import com.kavindu.techmart.common.enums.CircuitBreakerState;
import com.kavindu.techmart.common.exception.CircuitOpenException;
import com.kavindu.techmart.common.exception.TechMartException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircuitBreakerBeanTest {

    private CircuitBreakerBean breaker;

    private static final Callable<String> OK = () -> "ok";
    private static final Callable<String> BOOM = () -> {
        throw new RuntimeException("downstream failure");
    };

    @BeforeEach
    void setUp() {
        breaker = new CircuitBreakerBean();
        breaker.setFailureThreshold(3);
        breaker.setOpenTimeoutMs(30_000);
    }

    @Test
    @DisplayName("CLOSED state executes the operation and returns its result")
    void closedState_executes() {
        assertEquals("ok", breaker.callWithBreaker("svc", OK));
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState("svc"));
    }

    @Test
    @DisplayName("Reaching the failure threshold opens the circuit")
    void threshold_opensCircuit() {
        for (int i = 0; i < 3; i++) {
            assertThrows(TechMartException.class, () -> breaker.callWithBreaker("svc", BOOM));
        }
        assertEquals(CircuitBreakerState.OPEN, breaker.getState("svc"));
    }

    @Test
    @DisplayName("OPEN circuit fails fast with CircuitOpenException")
    void openState_throwsCircuitOpen() {
        for (int i = 0; i < 3; i++) {
            assertThrows(TechMartException.class, () -> breaker.callWithBreaker("svc", BOOM));
        }
        assertThrows(CircuitOpenException.class, () -> breaker.callWithBreaker("svc", OK));
    }

    @Test
    @DisplayName("HALF_OPEN + successful probe closes the circuit")
    void halfOpen_successCloses() {
        for (int i = 0; i < 3; i++) {
            assertThrows(TechMartException.class, () -> breaker.callWithBreaker("svc", BOOM));
        }
        breaker.setOpenTimeoutMs(0);
        assertEquals("ok", breaker.callWithBreaker("svc", OK));
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState("svc"));
    }

    @Test
    @DisplayName("HALF_OPEN + failed probe re-opens the circuit")
    void halfOpen_failureReopens() {
        for (int i = 0; i < 3; i++) {
            assertThrows(TechMartException.class, () -> breaker.callWithBreaker("svc", BOOM));
        }
        breaker.setOpenTimeoutMs(0);
        assertThrows(TechMartException.class, () -> breaker.callWithBreaker("svc", BOOM));
        assertEquals(CircuitBreakerState.OPEN, breaker.getState("svc"));
    }

    @Test
    @DisplayName("reset() forces the circuit back to CLOSED")
    void reset_closesCircuit() {
        for (int i = 0; i < 3; i++) {
            assertThrows(TechMartException.class, () -> breaker.callWithBreaker("svc", BOOM));
        }
        breaker.reset("svc");
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState("svc"));
        assertEquals("ok", breaker.callWithBreaker("svc", OK));
    }
}
