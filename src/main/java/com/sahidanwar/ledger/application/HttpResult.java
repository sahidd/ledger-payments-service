package com.sahidanwar.ledger.application;

import com.fasterxml.jackson.databind.JsonNode;

public record HttpResult(int status, JsonNode body) {
}
