package com.challenge.transfers.controller;

import com.challenge.transfers.model.api.DocumentType;
import com.challenge.transfers.model.api.RecipientsGetResponse;
import com.challenge.transfers.service.RecipientService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/transfers/customers-document")
public class RecipientsController {

  private final RecipientService recipientService;

  public RecipientsController(RecipientService recipientService) {
    this.recipientService = recipientService;
  }

  @GetMapping("/{customer-document-number}/recipients")
  public ResponseEntity<RecipientsGetResponse> getRecipients(
      @PathVariable("customer-document-number")
          @Pattern(regexp = "^\\d+$", message = "Document number must contain only digits")
          String customerDocument,
      @RequestParam("customer-document-type") DocumentType documentType) {
    return ResponseEntity.ok(recipientService.getRecipients(customerDocument, documentType));
  }
}
