package com.sahidanwar.ledger.api;

import com.sahidanwar.ledger.api.dto.ReconciliationResponse;
import com.sahidanwar.ledger.application.ReconciliationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ledger")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @GetMapping("/reconciliation")
    public ReconciliationResponse reconcile() {
        return reconciliationService.reconcile();
    }
}
