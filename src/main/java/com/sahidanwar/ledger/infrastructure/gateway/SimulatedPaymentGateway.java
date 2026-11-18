package com.sahidanwar.ledger.infrastructure.gateway;

import com.sahidanwar.ledger.domain.GatewayDecision;
import com.sahidanwar.ledger.domain.GatewayRequest;
import com.sahidanwar.ledger.domain.GatewayTransientException;
import com.sahidanwar.ledger.domain.PaymentGateway;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stand-in for an external card or bank gateway. A reference containing
 * {@code FORCE_DECLINE} is a permanent decline. {@code FORCE_TRANSIENT} throws
 * until Resilience4j exhausts retries and the circuit-breaker fallback runs.
 * The authorization id is derived from the idempotency key so a retried ledger
 * transaction asks the gateway for the same authorization.
 */
@Component
public class SimulatedPaymentGateway implements PaymentGateway {

    static final String FORCE_DECLINE = "FORCE_DECLINE";
    static final String FORCE_TRANSIENT = "FORCE_TRANSIENT";

    private final AtomicInteger attempts = new AtomicInteger();

    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "authorizeFallback")
    @Retry(name = "paymentGateway")
    public GatewayDecision authorize(GatewayRequest request) {
        attempts.incrementAndGet();
        String reference = request.reference() == null ? "" : request.reference();
        if (reference.contains(FORCE_TRANSIENT)) {
            throw new GatewayTransientException("simulated gateway timeout");
        }
        if (reference.contains(FORCE_DECLINE)) {
            return GatewayDecision.declined("The simulated gateway declined the payment.");
        }
        return GatewayDecision.approved("sim-" + request.idempotencyKey());
    }

    public GatewayDecision authorizeFallback(GatewayRequest request, Throwable failure) {
        return GatewayDecision.unavailable("Payment gateway is unavailable: " + failure.getMessage());
    }

    public int attempts() {
        return attempts.get();
    }

    public void resetAttempts() {
        attempts.set(0);
    }
}
