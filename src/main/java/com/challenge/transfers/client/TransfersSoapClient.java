package com.challenge.transfers.client;

import com.challenge.transfers.client.soap.GetAgendaCBU;
import com.challenge.transfers.client.soap.GetAgendaCBUResponse;
import com.challenge.transfers.client.soap.TerminalDTO;
import com.challenge.transfers.client.soap.UsuarioDTO;
import com.challenge.transfers.model.api.DocumentType;
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

    return (GetAgendaCBUResponse) getWebServiceTemplate().marshalSendAndReceive(request);
  }
}
