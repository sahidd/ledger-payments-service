package com.sahidanwar.ledger.api.dto;

import java.util.List;

public record ReconciliationResponse(boolean balanced, List<String> issues) {
}
