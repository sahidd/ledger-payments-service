package com.sahidanwar.ledger.domain;

/**
 * A retryable failure from the payment gateway. Resilience4j retries this exception
 * and the circuit breaker counts it. Business declines are not thrown as exceptions.
 */
public class GatewayTransientException extends RuntimeException {

    public GatewayTransientException(String message) {
        super(message);
    }
}
