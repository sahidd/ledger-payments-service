package com.sahidanwar.ledger.api;

import com.sahidanwar.ledger.api.dto.RiskExplanationResponse;
import com.sahidanwar.ledger.application.RiskExplanationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class RiskController {

    private final RiskExplanationService riskExplanationService;

    public RiskController(RiskExplanationService riskExplanationService) {
        this.riskExplanationService = riskExplanationService;
    }

    @GetMapping("/{id}/risk-explanation")
    public RiskExplanationResponse explain(@PathVariable UUID id) {
        return riskExplanationService.explain(id);
    }
}
