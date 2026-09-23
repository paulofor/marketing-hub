package com.marketinghub.experiment.run.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: garantir execução sequencial, auditável e idempotente do preflight técnico. */
class ExperimentTechnicalPreflightActivityExecutorTest {
  private final ProductProcessActivityPredecessorService predecessors =
      mock(ProductProcessActivityPredecessorService.class);
  private final ExperimentTechnicalPreflightEvidenceService evidenceService =
      mock(ExperimentTechnicalPreflightEvidenceService.class);
  private final BusinessProcessActivityInstanceRepository instances =
      mock(BusinessProcessActivityInstanceRepository.class);
  private final ObjectMapper json = new ObjectMapper();
  private final Product product = Product.builder().id(7L).build();
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final BusinessProcessActivityDefinition activity =
      new BusinessProcessActivityDefinition();
  private final ExperimentTechnicalPreflightActivityExecutor executor =
      new ExperimentTechnicalPreflightActivityExecutor(
          predecessors,
          evidenceService,
          instances,
          json,
          Clock.fixed(Instant.parse("2026-09-23T12:00:00Z"), ZoneOffset.UTC));

  /** Prepara uma atividade inicial e uma evidência vigente. */
  @BeforeEach
  void setup() {
    process.setId(58L);
    process.setProcessCode("experiment-homologation-activation");
    activity.setId(597L);
    activity.setActivityId("surfaces");
    when(predecessors.readiness(process, activity, "experiment:88"))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(true, "Primeira atividade pronta."));
    var objective =
        json.createObjectNode()
            .put("runId", 12L)
            .put("inputFingerprint", "fingerprint-v1")
            .put("activityId", "surfaces");
    when(evidenceService.evaluate("surfaces", product, "experiment:88"))
        .thenReturn(
            new ExperimentTechnicalPreflightEvidenceService.Evidence(
                88L, 12L, "fingerprint-v1", objective));
  }

  /** Registra uma conclusão de custo zero com a prova original preservada. */
  @Test
  void persistsVerifiedEvidenceWithZeroIncrementalCost() {
    var result = executor.execute(process, activity, product, "experiment:88");

    var saved = ArgumentCaptor.forClass(BusinessProcessActivityInstance.class);
    verify(instances).saveAndFlush(saved.capture());
    assertThat(saved.getValue().getStatus()).isEqualTo("COMPLETED");
    assertThat(saved.getValue().isObjectiveAchieved()).isTrue();
    assertThat(saved.getValue().getKnownCostUsd()).isZero();
    assertThat(saved.getValue().getCostCoverage()).isEqualTo("COMPLETE");
    assertThat(saved.getValue().getEvidenceQuality()).isEqualTo("REUSED_DIRECT");
    assertThat(saved.getValue().getEnteredAt()).isEqualTo(Instant.parse("2026-09-23T12:00:00Z"));
    assertThat(result.objectiveAchieved()).isTrue();
  }

  /** Não cria outra ocorrência quando run e impressão dos insumos permanecem iguais. */
  @Test
  void reusesIdenticalCompletedProjection() {
    var latest = new BusinessProcessActivityInstance();
    latest.setStatus("COMPLETED");
    latest.setObjectiveAchieved(true);
    latest.setObjectiveEvidenceJson("{\"runId\":12,\"inputFingerprint\":\"fingerprint-v1\"}");
    when(instances.findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            597L, "experiment:88"))
        .thenReturn(Optional.of(latest));

    executor.execute(process, activity, product, "experiment:88");

    verify(instances, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
  }

  /** Expõe o experimento e a prova atual para a interface sem produzir efeitos. */
  @Test
  void exposesReadinessForCurrentRun() {
    var readiness = executor.readiness(process, activity, product, "experiment:88");

    assertThat(readiness.ready()).isTrue();
    assertThat(readiness.workspaceCode()).isEqualTo("EXPERIMENT_PREFLIGHT");
    assertThat(readiness.workspaceReferenceId()).isEqualTo(88L);
    assertThat(readiness.requirements())
        .singleElement()
        .satisfies(r -> assertThat(r.satisfied()).isTrue());
  }

  /** Preserva a ordem do grafo quando a atividade anterior não foi comprovada. */
  @Test
  void blocksWhenPredecessorIsPending() {
    when(predecessors.readiness(process, activity, "experiment:88"))
        .thenReturn(
            new ProductProcessActivityPredecessorReadiness(false, "Conclua a atividade anterior."));

    var readiness = executor.readiness(process, activity, product, "experiment:88");

    assertThat(readiness.ready()).isFalse();
    assertThat(readiness.reason()).contains("atividade anterior");
    verify(evidenceService, never())
        .evaluate(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
  }

  /** Não captura atividades de outros processos ou códigos desconhecidos. */
  @Test
  void supportsOnlyCanonicalTechnicalActivities() {
    assertThat(executor.supports(process, activity)).isTrue();
    activity.setActivityId("unknown");
    assertThat(executor.supports(process, activity)).isFalse();
    process.setProcessCode("another-process");
    assertThat(executor.supports(process, activity)).isFalse();
  }
}
