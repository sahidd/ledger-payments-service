package com.sahidanwar.ledger.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.sahidanwar.ledger.api.dto.TransferRequest;
import com.sahidanwar.ledger.api.dto.TransferResponse;
import com.sahidanwar.ledger.application.HttpResult;
import com.sahidanwar.ledger.application.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<JsonNode> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        HttpResult result = transferService.transfer(idempotencyKey, request);
        MediaType contentType = result.status() >= 400
                ? MediaType.APPLICATION_PROBLEM_JSON
                : MediaType.APPLICATION_JSON;
        return ResponseEntity.status(result.status()).contentType(contentType).body(result.body());
    }

    @GetMapping("/{id}")
    public TransferResponse get(@PathVariable UUID id) {
        return transferService.get(id);
    }
}
