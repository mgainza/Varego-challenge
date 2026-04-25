package com.challenge.transfers.model.api;

public record AccountResponse(
    String cbu,
    AccountType code,
    String description,
    Boolean current,
    Boolean own) {}
