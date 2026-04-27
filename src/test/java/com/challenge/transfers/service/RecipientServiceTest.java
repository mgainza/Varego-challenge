package com.challenge.transfers.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.challenge.transfers.client.TransfersSoapClient;
import com.challenge.transfers.client.soap.GetAgendaCBUResponse;
import com.challenge.transfers.exception.SoapGatewayException;
import com.challenge.transfers.exception.SoapTimeoutException;
import com.challenge.transfers.mapper.RecipientMapper;
import com.challenge.transfers.model.api.DocumentType;
import com.challenge.transfers.model.api.RecipientsGetResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.WebServiceFaultException;
import org.springframework.ws.client.WebServiceIOException;

@ExtendWith(MockitoExtension.class)
class RecipientServiceTest {

  @Mock TransfersSoapClient soapClient;
  @Mock RecipientMapper mapper;

  RecipientService service;

  @BeforeEach
  void setUp() {
    service = new RecipientService(soapClient, mapper);
  }

  @Test
  void shouldReturnMappedResponse_whenSoapCallSucceeds() {
    GetAgendaCBUResponse soapResponse = new GetAgendaCBUResponse();
    RecipientsGetResponse expected = new RecipientsGetResponse(List.of());

    when(soapClient.getRecipientsCBU(eq("32345379"), eq(DocumentType.DNI)))
        .thenReturn(soapResponse);
    when(mapper.toRecipientsGetResponse(soapResponse)).thenReturn(expected);

    RecipientsGetResponse result = service.getRecipients("32345379", DocumentType.DNI);

    assertThat(result).isSameAs(expected);
    verify(soapClient).getRecipientsCBU("32345379", DocumentType.DNI);
    verify(mapper).toRecipientsGetResponse(soapResponse);
  }

  @Test
  void shouldThrowSoapTimeoutException_whenSoapClientThrowsIOException() {
    when(soapClient.getRecipientsCBU(any(), any()))
        .thenThrow(new WebServiceIOException("connection timed out", new IOException("timeout")));

    assertThatThrownBy(() -> service.getRecipients("32345379", DocumentType.DNI))
        .isInstanceOf(SoapTimeoutException.class)
        .hasMessageContaining("SOAP backend unreachable or timed out");
  }

  @Test
  void shouldThrowSoapGatewayException_whenSoapClientThrowsFaultException() {
    when(soapClient.getRecipientsCBU(any(), any())).thenThrow(mock(WebServiceFaultException.class));

    assertThatThrownBy(() -> service.getRecipients("32345379", DocumentType.DNI))
        .isInstanceOf(SoapGatewayException.class)
        .hasMessageContaining("SOAP backend returned a fault");
  }

  @Test
  void shouldThrowSoapGatewayException_whenMapperThrowsIllegalArgument() {
    when(soapClient.getRecipientsCBU(any(), any())).thenReturn(new GetAgendaCBUResponse());
    when(mapper.toRecipientsGetResponse(any()))
        .thenThrow(new IllegalArgumentException("Unknown account type code: 9"));

    assertThatThrownBy(() -> service.getRecipients("32345379", DocumentType.DNI))
        .isInstanceOf(SoapGatewayException.class)
        .hasMessageContaining("SOAP backend returned unrecognized data");
  }

  @Test
  void shouldPassDocumentAndTypeToSoapClient() {
    GetAgendaCBUResponse soapResponse = new GetAgendaCBUResponse();
    when(soapClient.getRecipientsCBU(any(), any())).thenReturn(soapResponse);
    when(mapper.toRecipientsGetResponse(any())).thenReturn(new RecipientsGetResponse(List.of()));

    service.getRecipients("20123456789", DocumentType.CUIT);

    verify(soapClient).getRecipientsCBU("20123456789", DocumentType.CUIT);
  }
}
