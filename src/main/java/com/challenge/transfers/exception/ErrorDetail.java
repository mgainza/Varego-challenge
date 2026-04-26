package com.challenge.transfers.exception;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorDetail(
    @JsonProperty("ErrorCode") String errorCode,
    @JsonProperty("Message") String message,
    @JsonProperty("Path") String path) {}
