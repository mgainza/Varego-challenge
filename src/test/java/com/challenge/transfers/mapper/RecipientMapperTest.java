package com.challenge.transfers.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.challenge.transfers.client.soap.AgendaCBUDTO;
import com.challenge.transfers.client.soap.GetAgendaCBUResponse;
import com.challenge.transfers.client.soap.PropiedadDTO;
import com.challenge.transfers.model.api.AccountType;
import com.challenge.transfers.model.api.RecipientsGetResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RecipientMapperTest {

  private final RecipientMapper mapper = new RecipientMapper();

  @Test
  void shouldNormalizeCuitWithSpacesToNull() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith(
                    "           ", "desc", "2850001040094075882478", propiedad("3", false, true))));

    assertThat(result.recipients().getFirst().cuit()).isNull();
  }

  @Test
  void shouldNormalizeCuitEmptyStringToNull() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith("", "desc", "2850001040094075882478", propiedad("3", false, true))));

    assertThat(result.recipients().getFirst().cuit()).isNull();
  }

  @Test
  void shouldNormalizeCuitNullToNull() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith(null, "desc", "2850001040094075882478", propiedad("3", false, true))));

    assertThat(result.recipients().getFirst().cuit()).isNull();
  }

  @Test
  void shouldPreserveCuitWhenValid() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith(
                    "20123456789", "desc", "2850001040094075882478", propiedad("3", false, true))));

    assertThat(result.recipients().getFirst().cuit()).isEqualTo("20123456789");
  }

  @Test
  void shouldTrimCuitWithSurroundingSpaces() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith(
                    "  20123456789  ",
                    "desc",
                    "2850001040094075882478",
                    propiedad("3", false, true))));

    assertThat(result.recipients().getFirst().cuit()).isEqualTo("20123456789");
  }

  @Test
  void shouldTrimDescripcion() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith(
                    null,
                    "test                  ",
                    "2850001040094075882478",
                    propiedad("3", false, true))));

    assertThat(result.recipients().getFirst().description()).isEqualTo("test");
  }

  @Test
  void shouldHandleNullDescripcion() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith(null, null, "2850001040094075882478", propiedad("3", false, true))));

    assertThat(result.recipients().getFirst().description()).isNull();
  }

  @Test
  void shouldHandleNullPropiedadDTO() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(agendaWith(null, "desc", "2850001040094075882478", null)));

    var account = result.recipients().getFirst().account();
    assertThat(account.cbu()).isEqualTo("2850001040094075882478");
    assertThat(account.code()).isNull();
    assertThat(account.description()).isNull();
    assertThat(account.current()).isNull();
    assertThat(account.own()).isNull();
  }

  @ParameterizedTest
  @CsvSource({"1, OWN_CHECKING", "2, NON_OWN_CHECKING", "3, OWN_OTHER", "4, NON_OWN_OTHER"})
  void shouldMapAccountTypeCorrectly(String codigo, AccountType expected) {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith(
                    null, "desc", "2850001040094075882478", propiedad(codigo, false, true))));

    assertThat(result.recipients().getFirst().account().code()).isEqualTo(expected);
  }

  @Test
  void shouldMapCtaCorrienteAndPropiaCorrectly() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith(null, "desc", "2850001040094075882478", propiedad("2", true, false))));

    var account = result.recipients().getFirst().account();
    assertThat(account.current()).isTrue();
    assertThat(account.own()).isFalse();
  }

  @Test
  void shouldMapNroCBUToCbu() {
    RecipientsGetResponse result =
        mapper.toRecipientsGetResponse(
            responseWith(
                agendaWith(null, "desc", "2850001040094075882478", propiedad("3", false, true))));

    assertThat(result.recipients().getFirst().account().cbu()).isEqualTo("2850001040094075882478");
  }

  @Test
  void shouldReturnEmptyListWhenNoAgendaCBU() {
    RecipientsGetResponse result = mapper.toRecipientsGetResponse(new GetAgendaCBUResponse());

    assertThat(result.recipients()).isEmpty();
  }

  @Test
  void shouldMapAllRecipientsInList() {
    GetAgendaCBUResponse response = new GetAgendaCBUResponse();
    response
        .getAgendaCBU()
        .add(
            agendaWith(
                "20111111111", "Lucas", "2850001040094075882478", propiedad("3", false, true)));
    response
        .getAgendaCBU()
        .add(
            agendaWith(
                "20222222222", "Ana", "2850001040094075882479", propiedad("4", false, false)));

    RecipientsGetResponse result = mapper.toRecipientsGetResponse(response);

    assertThat(result.recipients()).hasSize(2);
    assertThat(result.recipients().get(0).cuit()).isEqualTo("20111111111");
    assertThat(result.recipients().get(1).cuit()).isEqualTo("20222222222");
  }

  // --- helpers ---

  private GetAgendaCBUResponse responseWith(AgendaCBUDTO... items) {
    GetAgendaCBUResponse response = new GetAgendaCBUResponse();
    for (AgendaCBUDTO item : items) {
      response.getAgendaCBU().add(item);
    }
    return response;
  }

  private AgendaCBUDTO agendaWith(
      String cuit, String descripcion, String nroCBU, PropiedadDTO propiedad) {
    AgendaCBUDTO dto = new AgendaCBUDTO();
    dto.setCuitCuil(cuit);
    dto.setDescripcion(descripcion);
    dto.setNroCBU(nroCBU);
    dto.setPropiedadDTO(propiedad);
    return dto;
  }

  private PropiedadDTO propiedad(String codigo, boolean ctaCorriente, boolean propia) {
    PropiedadDTO dto = new PropiedadDTO();
    dto.setCodigo(codigo);
    dto.setCtaCorriente(ctaCorriente);
    dto.setPropia(propia);
    return dto;
  }
}
