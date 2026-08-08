package com.marketinghub.experiment.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.history.ExperimentHistoryEventContracts.CreateRequest;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: validar o registro factual e segregado do histórico de experimentos. */
@ExtendWith(MockitoExtension.class)
class ExperimentHistoryEventServiceTest {
  @Mock private ExperimentRepository experimentRepository;
  @Mock private ExperimentHistoryEventRepository historyRepository;
  private ExperimentHistoryEventService service;

  /** Monta o serviço isolado antes de cada cenário. */
  @BeforeEach
  void setUp() {
    service =
        new ExperimentHistoryEventService(
            experimentRepository, historyRepository, new ObjectMapper());
  }

  /** Registra métricas e evidências válidas sem perder a origem do fato. */
  @Test
  void createsAuditableHistoryEvent() {
    Experiment experiment = new Experiment();
    experiment.setId(85L);
    when(experimentRepository.findById(85L)).thenReturn(Optional.of(experiment));
    when(historyRepository.save(any()))
        .thenAnswer(
            invocation -> {
              ExperimentHistoryEvent event = invocation.getArgument(0);
              event.setId(1L);
              event.setCreatedAt(Instant.parse("2026-08-08T12:00:00Z"));
              return event;
            });

    var result =
        service.create(
            85L,
            new CreateRequest(
                "INCIDENTE",
                "Público incompatível",
                "A campanha alcançou público amplo sem a segmentação aprovada.",
                "{\"impressions\":182,\"clicks\":0}",
                "META_ADS",
                Instant.parse("2026-08-08T10:00:00Z")));

    assertThat(result.title()).isEqualTo("Público incompatível");
    assertThat(result.evidenceJson()).contains("182");
    assertThat(result.source()).isEqualTo("META_ADS");
  }

  /** Rejeita evidência inválida para manter o histórico consumível por agentes e relatórios. */
  @Test
  void rejectsInvalidEvidenceJson() {
    when(experimentRepository.findById(85L)).thenReturn(Optional.of(new Experiment()));
    assertThatThrownBy(
            () ->
                service.create(
                    85L,
                    new CreateRequest("INCIDENTE", "Falha", "Descrição", "não-json", null, null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("JSON válido");
  }

  /** Lista somente os registros associados ao experimento solicitado. */
  @Test
  void listsEventsIsolatedByExperiment() {
    when(experimentRepository.existsById(85L)).thenReturn(true);
    ExperimentHistoryEvent event = new ExperimentHistoryEvent();
    event.setId(2L);
    event.setCategory("DECISAO");
    event.setTitle("Pausar campanha");
    event.setDescription("Evitar gasto em teste inválido.");
    event.setSource("USUARIO");
    event.setOccurredAt(Instant.parse("2026-08-08T11:00:00Z"));
    event.setCreatedAt(Instant.parse("2026-08-08T11:01:00Z"));
    when(historyRepository.findByExperimentIdOrderByOccurredAtDescIdDesc(85L))
        .thenReturn(List.of(event));

    assertThat(service.list(85L)).extracting(r -> r.title()).containsExactly("Pausar campanha");
  }
}
