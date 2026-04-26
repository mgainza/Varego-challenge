package com.challenge.transfers.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.challenge.transfers.exception.SoapGatewayException;
import com.challenge.transfers.exception.SoapTimeoutException;
import com.challenge.transfers.model.api.AccountResponse;
import com.challenge.transfers.model.api.AccountType;
import com.challenge.transfers.model.api.DocumentType;
import com.challenge.transfers.model.api.RecipientResponse;
import com.challenge.transfers.model.api.RecipientsGetResponse;
import com.challenge.transfers.model.converter.DocumentTypeConverter;
import com.challenge.transfers.service.RecipientService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecipientsController.class)
@Import(DocumentTypeConverter.class)
class RecipientsControllerIntegrationTest {

  private static final String URL = "/v1/transfers/customers-document/{document}/recipients";

  @Autowired MockMvc mockMvc;

  @MockitoBean RecipientService recipientService;

  @Test
  void shouldReturn200_whenValidRequest() throws Exception {
    when(recipientService.getRecipients(eq("32345379"), eq(DocumentType.DNI)))
        .thenReturn(buildResponse());

    mockMvc
        .perform(get(URL, "32345379").param("customer-document-type", "01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recipient[0].cuit").value("20123456789"))
        .andExpect(jsonPath("$.recipient[0].description").value("Lucas Pérez"))
        .andExpect(jsonPath("$.recipient[0].account.cbu").value("2850001040094059465088"))
        .andExpect(jsonPath("$.recipient[0].account.code").value("OWN_CHECKING"))
        .andExpect(jsonPath("$.recipient[0].account.current").value(true))
        .andExpect(jsonPath("$.recipient[0].account.own").value(false));
  }

  @Test
  void shouldReturn400_whenDocumentContainsLetters() throws Exception {
    mockMvc
        .perform(get(URL, "ABC123").param("customer-document-type", "01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.Code").value("ERR-400"))
        .andExpect(jsonPath("$.Id").isNotEmpty())
        .andExpect(jsonPath("$.Errors[0].ErrorCode").value("ER-400"));
  }

  @Test
  void shouldReturn400_whenDocumentTypeIsInvalid() throws Exception {
    mockMvc
        .perform(get(URL, "32345379").param("customer-document-type", "99"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.Code").value("ERR-400"))
        .andExpect(
            jsonPath("$.Errors[0].Path")
                .value("/v1/transfers/customers-document/32345379/recipients"));
  }

  @Test
  void shouldReturn400_whenDocumentTypeMissing() throws Exception {
    mockMvc.perform(get(URL, "32345379")).andExpect(status().isBadRequest());
  }

  @Test
  void shouldReturn502_whenSoapFaultThrown() throws Exception {
    when(recipientService.getRecipients(any(), any()))
        .thenThrow(new SoapGatewayException("SOAP fault", new RuntimeException()));

    mockMvc
        .perform(get(URL, "32345379").param("customer-document-type", "01"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.Code").value("ERR-502"))
        .andExpect(jsonPath("$.Id").isNotEmpty())
        .andExpect(jsonPath("$.Errors[0].ErrorCode").value("ER-502"));
  }

  @Test
  void shouldReturn504_whenSoapTimeoutThrown() throws Exception {
    when(recipientService.getRecipients(any(), any()))
        .thenThrow(new SoapTimeoutException("timeout", new RuntimeException()));

    mockMvc
        .perform(get(URL, "32345379").param("customer-document-type", "01"))
        .andExpect(status().isGatewayTimeout())
        .andExpect(jsonPath("$.Code").value("ERR-504"))
        .andExpect(jsonPath("$.Id").isNotEmpty())
        .andExpect(jsonPath("$.Errors[0].ErrorCode").value("ER-504"));
  }

  @Test
  void shouldReturn500_whenUnexpectedExceptionThrown() throws Exception {
    when(recipientService.getRecipients(any(), any()))
        .thenThrow(new RuntimeException("unexpected"));

    mockMvc
        .perform(get(URL, "32345379").param("customer-document-type", "01"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.Code").value("ERR-500"))
        .andExpect(jsonPath("$.Errors[0].Message").value("An unexpected error occurred"));
  }

  @Test
  void shouldReturnUniqueIdOnEachErrorResponse() throws Exception {
    when(recipientService.getRecipients(any(), any()))
        .thenThrow(new SoapGatewayException("fault", new RuntimeException()));

    String id1 =
        mockMvc
            .perform(get(URL, "32345379").param("customer-document-type", "01"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id2 =
        mockMvc
            .perform(get(URL, "32345379").param("customer-document-type", "01"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(id1).isNotEqualTo(id2);
  }

  private static RecipientsGetResponse buildResponse() {
    AccountResponse account =
        new AccountResponse(
            "2850001040094059465088", AccountType.OWN_CHECKING, "Caja de Ahorro", true, false);
    RecipientResponse recipient = new RecipientResponse("20123456789", "Lucas Pérez", account);
    return new RecipientsGetResponse(List.of(recipient));
  }
}
