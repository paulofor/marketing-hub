package com.marketinghub.experiment.funnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.marketinghub.experiment.Experiment;
import com.marketinghub.repository.jpa.core.LeadRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentFunnelEventRepository;
import com.marketinghub.repository.jpa.experiment.funnel.ExperimentLandingAnalyticsEventRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Testa a limpeza operacional de eventos do funil quando o experimento precisa descartar dados de
 * teste.
 */
@ExtendWith(MockitoExtension.class)
class ExperimentFunnelServiceResetTest {

  @Mock private ExperimentRepository experimentRepository;

  @Mock private ExperimentFunnelEventRepository eventRepository;

  @Mock private ExperimentLandingAnalyticsEventRepository landingAnalyticsEventRepository;

  @Mock private LeadRepository leadRepository;

  @Mock private JdbcTemplate jdbcTemplate;

  @InjectMocks private ExperimentFunnelService service;

  /**
   * Valida que o reset apaga analytics de sessão antes dos demais eventos e atualiza o marco
   * temporal.
   */
  @Test
  void resetFunnelUpdatesTimestamp() {
    Experiment experiment = Experiment.builder().id(9L).build();
    when(experimentRepository.findById(9L)).thenReturn(Optional.of(experiment));

    Instant resetAt = service.resetFunnel(9L);

    InOrder inOrder =
        inOrder(landingAnalyticsEventRepository, eventRepository, experimentRepository);
    inOrder.verify(landingAnalyticsEventRepository).deleteByExperimentId(9L);
    inOrder
        .verify(eventRepository)
        .deleteByExperimentIdAndSource(
            9L, ExperimentFunnelEventRepository.LANDING_PAGE_ANALYTICS_SOURCE);
    inOrder.verify(eventRepository).deleteByExperimentId(9L);
    inOrder.verify(experimentRepository).save(experiment);
    assertNotNull(experiment.getFunnelResetAt());
    assertEquals(experiment.getFunnelResetAt(), resetAt);
  }

  /** Garante que o marco do reset seja comparado ao DATETIME do banco sem deslocamento de fuso. */
  @Test
  void convertsResetInstantToUtcDatabaseDateTime() {
    Instant resetAt = Instant.parse("2026-08-07T16:55:40.604464Z");

    LocalDateTime databaseBaseline = ExperimentFunnelService.toUtcDatabaseDateTime(resetAt);

    assertEquals(LocalDateTime.parse("2026-08-07T16:55:40.604464"), databaseBaseline);
  }

  /** Garante que o DATETIME UTC do banco não receba deslocamento do fuso da JVM na leitura. */
  @Test
  void convertsUtcDatabaseDateTimeBackToInstant() {
    LocalDateTime databaseValue = LocalDateTime.parse("2026-08-22T14:44:26.729474");

    Instant instant = ExperimentFunnelService.fromUtcDatabaseDateTime(databaseValue);

    assertEquals(Instant.parse("2026-08-22T14:44:26.729474Z"), instant);
  }

  /** Normaliza a projeção com offset usada pelo banco de testes para o mesmo instante UTC. */
  @Test
  void convertsOffsetProjectionToUtcInstant() {
    OffsetDateTime databaseValue = OffsetDateTime.parse("2026-08-22T11:44:26.729474-03:00");

    Instant instant = ExperimentFunnelService.fromUtcDatabaseValue(databaseValue);

    assertEquals(Instant.parse("2026-08-22T14:44:26.729474Z"), instant);
  }

  /** Interpreta o timestamp JDBC de DATETIME como UTC sem aplicar o fuso da JVM. */
  @Test
  void convertsJdbcTimestampToUtcInstant() {
    Timestamp databaseValue = Timestamp.valueOf(LocalDateTime.parse("2026-08-22T14:44:26.729474"));

    Instant instant = ExperimentFunnelService.fromUtcDatabaseValue(databaseValue);

    assertEquals(Instant.parse("2026-08-22T14:44:26.729474Z"), instant);
  }
}
