package com.sahidanwar.ledger.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sahidanwar.ledger.domain.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class ProblemResponses {

    private final ObjectMapper objectMapper;

    public ProblemResponses(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public HttpResult result(DomainException exception, URI instance) {
        int status = status(exception.code()).value();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("type", type(exception.code()).toString());
        body.put("title", title(exception.code()));
        body.put("status", status);
        body.put("detail", exception.getMessage());
        body.put("instance", instance.toString());
        return new HttpResult(status, body);
    }

    public ProblemDetail problem(DomainException exception, URI instance) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status(exception.code()), exception.getMessage());
        detail.setTitle(title(exception.code()));
        detail.setType(type(exception.code()));
        detail.setInstance(instance);
        return detail;
    }

    public HttpStatus status(String code) {
        return switch (code) {
            case "not-found" -> HttpStatus.NOT_FOUND;
            case "insufficient-funds", "currency-mismatch", "same-account" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "account-not-active", "idempotency-conflict", "concurrent-modification" -> HttpStatus.CONFLICT;
            case "invalid-amount", "invalid-idempotency-key" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    public String title(String code) {
        return switch (code) {
            case "not-found" -> "Not found";
            case "insufficient-funds" -> "Insufficient funds";
            case "currency-mismatch" -> "Currency mismatch";
            case "same-account" -> "Same account";
            case "account-not-active" -> "Account not active";
            case "idempotency-conflict" -> "Idempotency conflict";
            case "concurrent-modification" -> "Concurrent modification";
            case "invalid-amount" -> "Invalid amount";
            case "invalid-idempotency-key" -> "Invalid idempotency key";
            default -> "Request rejected";
        };
    }

    private URI type(String code) {
        return URI.create("https://ledger.sahidanwar.dev/problems/" + code);
    }
}
