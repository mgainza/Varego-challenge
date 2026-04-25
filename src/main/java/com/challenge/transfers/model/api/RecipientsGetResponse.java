package com.challenge.transfers.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record RecipientsGetResponse(
        @JsonProperty("recipient")
        List<RecipientResponse> recipients
) {}
