package com.sahidanwar.ledger.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank @Size(max = 200) String ownerName,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotNull @DecimalMin("0.00") @Digits(integer = 15, fraction = 2) BigDecimal initialBalance
) {
}
