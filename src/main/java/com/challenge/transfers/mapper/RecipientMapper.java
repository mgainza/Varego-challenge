package com.challenge.transfers.mapper;

import com.challenge.transfers.client.soap.AgendaCBUDTO;
import com.challenge.transfers.client.soap.GetAgendaCBUResponse;
import com.challenge.transfers.client.soap.PropiedadDTO;
import com.challenge.transfers.model.api.AccountResponse;
import com.challenge.transfers.model.api.AccountType;
import com.challenge.transfers.model.api.RecipientResponse;
import com.challenge.transfers.model.api.RecipientsGetResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RecipientMapper {

  public RecipientsGetResponse toRecipientsGetResponse(GetAgendaCBUResponse source) {
    List<RecipientResponse> recipients =
        source.getAgendaCBU().stream().map(this::toRecipientResponse).toList();
    return new RecipientsGetResponse(recipients);
  }

  private RecipientResponse toRecipientResponse(AgendaCBUDTO source) {
    return new RecipientResponse(
        normalizeCuit(source.getCuitCuil()),
        source.getDescripcion() != null ? source.getDescripcion().trim() : null,
        toAccountResponse(source));
  }

  private AccountResponse toAccountResponse(AgendaCBUDTO source) {
    PropiedadDTO propiedad = source.getPropiedadDTO();
    if (propiedad == null) {
      return new AccountResponse(source.getNroCBU(), null, null, null, null);
    }
    return new AccountResponse(
        source.getNroCBU(),
        AccountType.fromCode(propiedad.getCodigo()),
        propiedad.getDescripcion(),
        propiedad.isCtaCorriente(),
        propiedad.isPropia());
  }

  private String normalizeCuit(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
