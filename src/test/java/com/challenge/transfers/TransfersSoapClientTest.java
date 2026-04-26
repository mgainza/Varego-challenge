package com.challenge.transfers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.challenge.transfers.client.TransfersSoapClient;
import com.challenge.transfers.client.soap.GetAgendaCBU;
import com.challenge.transfers.client.soap.GetAgendaCBUResponse;
import com.challenge.transfers.model.api.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ws.client.core.WebServiceTemplate;

@ExtendWith(MockitoExtension.class)
class TransfersSoapClientTest {

  @Mock WebServiceTemplate webServiceTemplate;

  TransfersSoapClient client;

  @BeforeEach
  void setUp() {
    client = new TransfersSoapClient("WEB", "MS-TRANSFERS", "secret", "127.0.0.1");
    client.setWebServiceTemplate(webServiceTemplate);
  }

  @Test
  void shouldReturnResponse_whenSoapCallSucceeds() {
    GetAgendaCBUResponse mockResponse = new GetAgendaCBUResponse();
    when(webServiceTemplate.marshalSendAndReceive(any(GetAgendaCBU.class)))
        .thenReturn(mockResponse);

    GetAgendaCBUResponse result = client.getRecipientsCBU("32345379", DocumentType.DNI);

    assertThat(result).isNotNull();
  }

  @Test
  void shouldBuildRequestWithCorrectUserData() {
    when(webServiceTemplate.marshalSendAndReceive(any(GetAgendaCBU.class)))
        .thenReturn(new GetAgendaCBUResponse());

    ArgumentCaptor<GetAgendaCBU> captor = ArgumentCaptor.forClass(GetAgendaCBU.class);

    client.getRecipientsCBU("32345379", DocumentType.DNI);

    verify(webServiceTemplate).marshalSendAndReceive(captor.capture());
    GetAgendaCBU request = captor.getValue();

    assertThat(request.getUsuario().getNroDocumento()).isEqualTo("32345379");
    assertThat(request.getUsuario().getTipoDocumento()).isEqualTo("01");
    assertThat(request.getUsuario().getPassword()).isEqualTo("secret");
  }

  @Test
  void shouldBuildRequestWithCorrectTerminalData() {
    when(webServiceTemplate.marshalSendAndReceive(any(GetAgendaCBU.class)))
        .thenReturn(new GetAgendaCBUResponse());

    ArgumentCaptor<GetAgendaCBU> captor = ArgumentCaptor.forClass(GetAgendaCBU.class);

    client.getRecipientsCBU("32345379", DocumentType.DNI);

    verify(webServiceTemplate).marshalSendAndReceive(captor.capture());
    GetAgendaCBU request = captor.getValue();

    assertThat(request.getTerminal().getCanal()).isEqualTo("WEB");
    assertThat(request.getTerminal().getTerminal()).isEqualTo("MS-TRANSFERS");
    assertThat(request.getTerminal().getDireccionIp()).isEqualTo("127.0.0.1");
  }
}
