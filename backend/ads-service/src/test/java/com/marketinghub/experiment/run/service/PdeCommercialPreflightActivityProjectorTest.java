package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.BusinessProcessActivityInstance;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorReadiness;
import com.marketinghub.businessprocess.execution.service.predecessor.ProductProcessActivityPredecessorService;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.run.ExperimentRun;
import com.marketinghub.experiment.run.ExperimentRunGateResult;
import com.marketinghub.experiment.run.ExperimentRunGateStatus;
import com.marketinghub.experiment.run.ExperimentRunMode;
import com.marketinghub.experiment.run.ExperimentRunStatus;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessActivityDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Responsabilidade: comprovar a projeção idempotente do run na atividade comercial de preflight.
 */
class PdeCommercialPreflightActivityProjectorTest {
  private static final Instant NOW = Instant.parse("2026-08-30T16:00:00Z");
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final BusinessProcessActivityDefinitionRepository activities =
      mock(BusinessProcessActivityDefinitionRepository.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PdeCommercialPreflightActivityProjector projector =
      new PdeCommercialPreflightActivityProjector(
          processes,
          activities,
          instances,
          predecessors,
          objectMapper,
          Clock.fixed(NOW, ZoneOffset.UTC));

  /** Conclui a atividade quando o run produtivo atinge READY_TO_PUBLISH. */
  @Test
  void completesActivityFromReadyRun() throws Exception {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    ExperimentRun run = run(ExperimentRunStatus.READY_TO_PUBLISH);
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "pde-commercial-homologation-activation", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    when(activities.findByProcessDefinitionIdAndActivityId(56L, "preflight"))
        .thenReturn(Optional.of(activity));
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Revisões concluídas."));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            589L, "experiment:89"))
        .thenReturn(Optional.empty());

    projector.synchronize(run, List.of(gate(ExperimentRunGateStatus.PASS, "Tudo aprovado.")));

    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).save(saved.capture());
    assertThat(saved.getValue().getStatus()).isEqualTo("COMPLETED");
    assertThat(saved.getValue().isObjectiveAchieved()).isTrue();
    assertThat(saved.getValue().getExitedAt()).isEqualTo(NOW);
    var evidence = objectMapper.readTree(saved.getValue().getObjectiveEvidenceJson());
    assertThat(evidence.path("runId").asLong()).isEqualTo(12L);
    assertThat(evidence.path("runStatus").asText()).isEqualTo("READY_TO_PUBLISH");
    assertThat(evidence.path("blockerCount").asInt()).isZero();
  }

  /** Não cria a atividade quando as revisões anteriores ainda não concluíram. */
  @Test
  void doesNotProjectBeforePredecessors() {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    ExperimentRun run = run(ExperimentRunStatus.PREFLIGHT_PENDING);
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(any(), any()))
        .thenReturn(Optional.of(process));
    when(activities.findByProcessDefinitionIdAndActivityId(56L, "preflight"))
        .thenReturn(Optional.of(activity));
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(
                false, "Conclua primeiro a revisão comercial."));

    projector.synchronize(run, List.of());

    verify(instances, never()).save(any());
  }

  /** Cria nova ocorrência para o run seguinte e preserva o bloqueio da tentativa anterior. */
  @Test
  void preservesBlockedOccurrenceWhenRetryRunCompletes() throws Exception {
    BusinessProcessDefinition process = process();
    BusinessProcessActivityDefinition activity = activity(process);
    ExperimentRun retryRun = run(ExperimentRunStatus.READY_TO_PUBLISH);
    retryRun.setId(13L);
    retryRun.setRunNumber(2);
    BusinessProcessActivityInstance blocked = new BusinessProcessActivityInstance();
    blocked.setId(148L);
    blocked.setActivityDefinition(activity);
    blocked.setSourceReference("experiment:89");
    blocked.setOccurrenceNumber(1);
    blocked.setStatus("BLOCKED");
    blocked.setObjectiveAchieved(false);
    blocked.setObjectiveEvidenceJson("{\"runId\":12}");
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "pde-commercial-homologation-activation", "PUBLISHED"))
        .thenReturn(Optional.of(process));
    when(activities.findByProcessDefinitionIdAndActivityId(56L, "preflight"))
        .thenReturn(Optional.of(activity));
    when(predecessors.readiness(process, activity, "experiment:89"))
        .thenReturn(new ProductProcessActivityPredecessorReadiness(true, "Revisões concluídas."));
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            589L, "experiment:89"))
        .thenReturn(Optional.of(blocked));

    projector.synchronize(retryRun, List.of(gate(ExperimentRunGateStatus.PASS, "Tudo aprovado.")));

    ArgumentCaptor<BusinessProcessActivityInstance> saved =
        ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).save(saved.capture());
    assertThat(saved.getValue()).isNotSameAs(blocked);
    assertThat(saved.getValue().getOccurrenceNumber()).isEqualTo(2);
    assertThat(saved.getValue().getStatus()).isEqualTo("COMPLETED");
    assertThat(
            objectMapper
                .readTree(saved.getValue().getObjectiveEvidenceJson())
                .path("runId")
                .asLong())
        .isEqualTo(13L);
    assertThat(blocked.getStatus()).isEqualTo("BLOCKED");
  }

  /** Monta o processo publicado que contém a atividade de preflight. */
  private BusinessProcessDefinition process() {
    BusinessProcessDefinition process = new BusinessProcessDefinition();
    process.setId(56L);
    process.setProcessCode("pde-commercial-homologation-activation");
    process.setStatus("PUBLISHED");
    return process;
  }

  /** Monta a definição relacional da atividade projetada. */
  private BusinessProcessActivityDefinition activity(BusinessProcessDefinition process) {
    BusinessProcessActivityDefinition activity = new BusinessProcessActivityDefinition();
    activity.setId(589L);
    activity.setProcessDefinition(process);
    activity.setActivityId("preflight");
    return activity;
  }

  /** Monta um run produtivo vinculado ao produto Rigel. */
  private ExperimentRun run(ExperimentRunStatus status) {
    Product product = Product.builder().id(9L).build();
    Experiment experiment = new Experiment();
    experiment.setId(89L);
    experiment.setProduct(product);
    return ExperimentRun.builder()
        .id(12L)
        .experiment(experiment)
        .runNumber(1)
        .mode(ExperimentRunMode.PRODUCTION)
        .status(status)
        .requestedAt(Instant.parse("2026-08-30T15:50:00Z"))
        .preflightStartedAt(Instant.parse("2026-08-30T15:51:00Z"))
        .preflightCompletedAt(
            status == ExperimentRunStatus.READY_TO_PUBLISH
                ? Instant.parse("2026-08-30T15:59:00Z")
                : null)
        .build();
  }

  /** Monta um gate mínimo para comprovar a contagem projetada. */
  private ExperimentRunGateResult gate(ExperimentRunGateStatus status, String summary) {
    return ExperimentRunGateResult.builder().status(status).summary(summary).build();
  }
}
