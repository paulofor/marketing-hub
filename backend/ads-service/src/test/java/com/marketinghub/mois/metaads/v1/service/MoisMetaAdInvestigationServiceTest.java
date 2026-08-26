package com.marketinghub.mois.metaads.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Protege o modo de coleta comercial brasileiro e seu objetivo temporal. */
class MoisMetaAdInvestigationServiceTest {

  /** Deve orientar reobservação trinta dias após a primeira evidência real. */
  @Test
  void schedulesSupervisedReobservationAfterThirtyDays() {
    MoisMetaAdInvestigationService service =
        new MoisMetaAdInvestigationService(mock(JdbcTemplate.class), new ObjectMapper());

    MoisMetaAdDtos.CollectionState state =
        service.collectionState(
            "BR",
            "INSTAGRAM",
            "entrevista emprego",
            Instant.parse("2026-08-05T12:00:00Z"),
            Instant.parse("2026-08-03T12:00:00Z"),
            null);

    assertThat(state.mode()).isEqualTo("SUPERVISED");
    assertThat(state.reason()).contains("não disponibiliza anúncios comerciais gerais");
    assertThat(state.nextObservationAt()).isEqualTo(Instant.parse("2026-09-04T12:00:00Z"));
    assertThat(state.searchUrl()).contains("country=BR", "q=entrevista+emprego");
  }

  /** Deve pedir a primeira observação imediatamente quando o radar ainda está vazio. */
  @Test
  void requestsFirstSupervisedObservationImmediately() {
    MoisMetaAdInvestigationService service =
        new MoisMetaAdInvestigationService(mock(JdbcTemplate.class), new ObjectMapper());
    Instant createdAt = Instant.parse("2026-08-24T12:00:00Z");

    MoisMetaAdDtos.CollectionState state =
        service.collectionState("BR", "INSTAGRAM", "entrevista emprego", null, createdAt, null);

    assertThat(state.nextObservationAt()).isEqualTo(createdAt);
  }

  /** Deve usar a API oficial somente nos territórios comerciais cobertos pela Meta. */
  @Test
  void selectsOfficialApiOnlyForCoveredTerritory() {
    MoisMetaAdInvestigationService service =
        new MoisMetaAdInvestigationService(mock(JdbcTemplate.class), new ObjectMapper());

    assertThat(service.supportsOfficialCommercialApi("PT")).isTrue();
    assertThat(service.supportsOfficialCommercialApi("BR")).isFalse();
  }
}
