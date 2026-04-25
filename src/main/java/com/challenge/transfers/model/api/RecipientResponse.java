package com.challenge.transfers.model.api;

public record RecipientResponse(
    String cuit,
    String description,
    AccountResponse account) {}
