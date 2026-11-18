package com.sahidanwar.ledger.domain;

public interface PaymentGateway {

    GatewayDecision authorize(GatewayRequest request);
}
