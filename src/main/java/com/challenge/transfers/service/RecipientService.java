package com.challenge.transfers.service;

import com.challenge.transfers.client.TransfersSoapClient;
import com.challenge.transfers.client.soap.GetAgendaCBUResponse;
import com.challenge.transfers.exception.SoapGatewayException;
import com.challenge.transfers.exception.SoapTimeoutException;
import com.challenge.transfers.mapper.RecipientMapper;
import com.challenge.transfers.model.api.DocumentType;
import com.challenge.transfers.model.api.RecipientsGetResponse;
import com.challenge.transfers.util.LogMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.WebServiceFaultException;
import org.springframework.ws.client.WebServiceIOException;

@Service
public class RecipientService {

  private static final Logger log = LoggerFactory.getLogger(RecipientService.class);

  private final TransfersSoapClient transfersSoapClient;
  private final RecipientMapper recipientMapper;

  public RecipientService(
      TransfersSoapClient transfersSoapClient, RecipientMapper recipientMapper) {
    this.transfersSoapClient = transfersSoapClient;
    this.recipientMapper = recipientMapper;
  }

  public RecipientsGetResponse getRecipients(
      String customerDocument, DocumentType customerDocumentType) {

    log.info(
        "Fetching recipients [document={}, documentType={}]",
        LogMask.document(customerDocument),
        customerDocumentType);

    GetAgendaCBUResponse soapResponse;
    try {
      soapResponse = transfersSoapClient.getRecipientsCBU(customerDocument, customerDocumentType);
    } catch (WebServiceIOException ex) {
      throw new SoapTimeoutException("SOAP backend unreachable or timed out", ex);
    } catch (WebServiceFaultException ex) {
      throw new SoapGatewayException("SOAP backend returned a fault", ex);
    }

    RecipientsGetResponse response;
    try {
      response = recipientMapper.toRecipientsGetResponse(soapResponse);
    } catch (IllegalArgumentException ex) {
      throw new SoapGatewayException(
          "SOAP backend returned unrecognized data: " + ex.getMessage(), ex);
    }

    log.info(
        "Recipients fetched [document={}, count={}]",
        LogMask.document(customerDocument),
        response.recipients().size());

    return response;
  }
}
