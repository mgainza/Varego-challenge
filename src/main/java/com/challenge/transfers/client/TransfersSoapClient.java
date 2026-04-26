package com.challenge.transfers.client;

import com.challenge.transfers.client.soap.*;
import com.challenge.transfers.model.api.DocumentType;
import jakarta.xml.bind.JAXBElement;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

public class TransfersSoapClient extends WebServiceGatewaySupport {

  private final String channel;
  private final String terminal;
  private final String password;
  private final String ip;

  public TransfersSoapClient(String channel, String terminal, String password, String ip) {
    this.channel = channel;
    this.terminal = terminal;
    this.password = password;
    this.ip = ip;
  }

  public GetAgendaCBUResponse getRecipientsCBU(
      String customerDocument, DocumentType customerDocumentType) {

    UsuarioDTO usuario = new UsuarioDTO();
    usuario.setNroDocumento(customerDocument);
    usuario.setTipoDocumento(customerDocumentType.getCode());
    usuario.setPassword(password);

    TerminalDTO terminalDTO = new TerminalDTO();
    terminalDTO.setTerminal(terminal);
    terminalDTO.setCanal(channel);
    terminalDTO.setDireccionIp(ip);
    GetAgendaCBU request = new GetAgendaCBU();
    request.setUsuario(usuario);
    request.setTerminal(terminalDTO);

    ObjectFactory objectFactory = new ObjectFactory();

    Object raw =
        getWebServiceTemplate().marshalSendAndReceive(objectFactory.createGetAgendaCBU(request));

    if (raw instanceof JAXBElement<?> jaxbElement) {
      return (GetAgendaCBUResponse) jaxbElement.getValue();
    }
    return (GetAgendaCBUResponse) raw;
  }
}
