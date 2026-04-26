package com.challenge.transfers.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ErrorResponse(
    @JsonProperty("Code") String code,
    @JsonProperty("Id") String id,
    @JsonProperty("Message") String message,
    @JsonProperty("Errors") List<ErrorDetail> errors) {}
