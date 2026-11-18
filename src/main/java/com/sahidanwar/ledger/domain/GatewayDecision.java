package com.sahidanwar.ledger.domain;

public record GatewayDecision(boolean approved, boolean unavailable, String authId, String reason) {

    public static GatewayDecision approved(String authId) {
        return new GatewayDecision(true, false, authId, null);
    }

    public static GatewayDecision declined(String reason) {
        return new GatewayDecision(false, false, null, reason);
    }

    public static GatewayDecision unavailable(String reason) {
        return new GatewayDecision(false, true, null, reason);
    }
}
