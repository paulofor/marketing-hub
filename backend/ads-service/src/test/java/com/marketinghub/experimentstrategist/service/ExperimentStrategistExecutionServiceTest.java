package com.marketinghub.experimentstrategist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experimentstrategist.ExperimentStrategistBehavioralSnapshot;
import com.marketinghub.experimentstrategist.ExperimentStrategistBehavioralSnapshotStatus;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecution;
import com.marketinghub.experimentstrategist.ExperimentStrategistExecutionStatus;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistBehavioralSnapshotRepository;
import com.marketinghub.repository.jpa.experimentstrategist.ExperimentStrategistExecutionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

/** Responsabilidade: validar fila e congelamento auditavel da pesquisa estrategica. */
class ExperimentStrategistExecutionServiceTest {
  /** Retoma uma lease órfã uma única vez antes de reservar o próximo trabalho. */
  @Test
  void recoversStaleExecutionBeforeClaiming() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(7L);
    ExperimentStrategistExecution stale = execution(12L, plan);
    stale.setStartedAt(Instant.now().minusSeconds(600));
    when(repository.findByStatusAndStartedAtBeforeOrderByStartedAtAsc(any(), any()))
        .thenReturn(List.of(stale));
    when(repository.countRecentActiveTelemetry(any(), any())).thenReturn(0L);
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.findByStatusOrderByCreatedAtAsc(any(), any(Pageable.class)))
        .thenReturn(List.of(stale));
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(
            repository,
            mock(ExperimentStrategistBehavioralSnapshotRepository.class),
            mock(CommercialPlanService.class),
            mock(ExperimentStrategistContextService.class),
            new ObjectMapper());

    var claimed = service.claim();

    assertThat(claimed.status()).isEqualTo(ExperimentStrategistExecutionStatus.RUNNING);
    assertThat(stale.getErrorMessage()).isNull();
    verify(repository).countRecentActiveTelemetry(any(), any());
  }

  /** Entrega ao MCP as evidencias congeladas da execucao estrategica reservada. */
  @Test
  void exposesFrozenExecutionEvidenceToMcp() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(7L);
    ExperimentStrategistExecution execution = new ExperimentStrategistExecution();
    execution.setId(12L);
    execution.setCommercialPlan(plan);
    execution.setStatus(ExperimentStrategistExecutionStatus.RUNNING);
    execution.setResearchQuestion("Qual oferta testar?");
    execution.setEvidenceSnapshot("{\"bottleneck\":\"CHECKOUT\"}");
    when(repository.findById(12L)).thenReturn(Optional.of(execution));
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(
            repository,
            mock(ExperimentStrategistBehavioralSnapshotRepository.class),
            mock(CommercialPlanService.class),
            mock(ExperimentStrategistContextService.class),
            new ObjectMapper());

    var response = service.getExecution(12L);

    assertThat(response.id()).isEqualTo(12L);
    assertThat(response.evidenceSnapshot()).contains("CHECKOUT");
  }

  /** Cria uma pendencia somente leitura com a evidencia congelada. */
  @Test
  void startsPendingResearchWithFrozenEvidence() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlanService plans = mock(CommercialPlanService.class);
    ExperimentStrategistContextService contexts = mock(ExperimentStrategistContextService.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(7L);
    when(plans.getPlan(7L)).thenReturn(plan);
    when(contexts.researchContext(7L)).thenReturn(Map.of("bottleneck", "CHECKOUT"));
    when(repository.save(any()))
        .thenAnswer(
            invocation -> {
              ExperimentStrategistExecution value = invocation.getArgument(0);
              value.setId(12L);
              value.setCreatedAt(Instant.now());
              return value;
            });
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(
            repository,
            mock(ExperimentStrategistBehavioralSnapshotRepository.class),
            plans,
            contexts,
            new ObjectMapper());

    var result =
        service.start(
            7L, new ExperimentStrategistExecutionService.StartRequest("Qual oferta testar?"));

    assertThat(result.status()).isEqualTo(ExperimentStrategistExecutionStatus.PENDING);
    assertThat(result.authorityMode()).isEqualTo("READ_ONLY_RESEARCH");
    assertThat(result.evidenceSnapshot()).contains("CHECKOUT");
    verify(repository).save(any(ExperimentStrategistExecution.class));
  }

  /** Reserva e conclui somente snapshot agregado do experimento pertencente ao plano. */
  @Test
  void persistsSegregatedClaritySnapshotWithZeroProviderCost() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    ExperimentStrategistBehavioralSnapshotRepository snapshots =
        mock(ExperimentStrategistBehavioralSnapshotRepository.class);
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(2L);
    plan.setExperiment(experiment);
    ExperimentStrategistExecution execution = new ExperimentStrategistExecution();
    execution.setId(19L);
    execution.setCommercialPlan(plan);
    execution.setStatus(ExperimentStrategistExecutionStatus.RUNNING);
    when(repository.findById(19L)).thenReturn(Optional.of(execution));
    AtomicReference<ExperimentStrategistBehavioralSnapshot> persisted = new AtomicReference<>();
    when(snapshots.save(any()))
        .thenAnswer(
            invocation -> {
              ExperimentStrategistBehavioralSnapshot value = invocation.getArgument(0);
              value.setId(44L);
              persisted.set(value);
              return value;
            });
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(
            repository,
            snapshots,
            mock(CommercialPlanService.class),
            mock(ExperimentStrategistContextService.class),
            new ObjectMapper());

    var reserved =
        service.reserveBehavioralSnapshot(
            19L,
            new ExperimentStrategistExecutionService.ReserveBehavioralSnapshotRequest(
                88L, "DEVICE", 2));

    assertThat(reserved.experimentId()).isEqualTo(88L);
    assertThat(reserved.queryText())
        .contains("/flows/exp-88-", "tipo de dispositivo", "Não retorne gravações");
    assertThat(reserved.estimatedCostUsd()).isEqualByComparingTo(BigDecimal.ZERO);

    when(snapshots.findByIdAndExecutionId(44L, 19L))
        .thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.completeBehavioralSnapshot(
                    19L,
                    44L,
                    new ExperimentStrategistExecutionService.CompleteBehavioralSnapshotRequest(
                        "{\"sessionId\":\"individual\"}")))
        .hasMessageContaining("campo individual proibido");

    var completed =
        service.completeBehavioralSnapshot(
            19L,
            44L,
            new ExperimentStrategistExecutionService.CompleteBehavioralSnapshotRequest(
                "{\"sessions\":12,\"rageClicks\":2}"));
    assertThat(completed.status())
        .isEqualTo(ExperimentStrategistBehavioralSnapshotStatus.COMPLETED);
  }

  /** Bloqueia a quarta consulta da mesma execução antes de consumir a cota externa. */
  @Test
  void blocksFourthClaritySnapshotPerExecution() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    ExperimentStrategistBehavioralSnapshotRepository snapshots =
        mock(ExperimentStrategistBehavioralSnapshotRepository.class);
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    CommercialPlan plan = new CommercialPlan();
    plan.setExperiment(experiment);
    ExperimentStrategistExecution execution = new ExperimentStrategistExecution();
    execution.setId(19L);
    execution.setCommercialPlan(plan);
    execution.setStatus(ExperimentStrategistExecutionStatus.RUNNING);
    when(repository.findById(19L)).thenReturn(Optional.of(execution));
    when(snapshots.countByExecutionId(19L)).thenReturn(3L);
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(
            repository,
            snapshots,
            mock(CommercialPlanService.class),
            mock(ExperimentStrategistContextService.class),
            new ObjectMapper());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.reserveBehavioralSnapshot(
                    19L,
                    new ExperimentStrategistExecutionService.ReserveBehavioralSnapshotRequest(
                        88L, "PAGE", 1)))
        .hasMessageContaining("três snapshots");
  }

  /** Preserva uma consulta de folga e bloqueia o décimo uso diário da API externa. */
  @Test
  void blocksTenthClaritySnapshotOfUtcDay() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    ExperimentStrategistBehavioralSnapshotRepository snapshots =
        mock(ExperimentStrategistBehavioralSnapshotRepository.class);
    Experiment experiment = new Experiment();
    experiment.setId(88L);
    CommercialPlan plan = new CommercialPlan();
    plan.setExperiment(experiment);
    ExperimentStrategistExecution execution = new ExperimentStrategistExecution();
    execution.setId(19L);
    execution.setCommercialPlan(plan);
    execution.setStatus(ExperimentStrategistExecutionStatus.RUNNING);
    when(repository.findById(19L)).thenReturn(Optional.of(execution));
    when(snapshots.countByExecutionId(19L)).thenReturn(0L);
    when(snapshots.countByProviderAndRequestedAtGreaterThanEqual(any(), any())).thenReturn(9L);
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(
            repository,
            snapshots,
            mock(CommercialPlanService.class),
            mock(ExperimentStrategistContextService.class),
            new ObjectMapper());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.reserveBehavioralSnapshot(
                    19L,
                    new ExperimentStrategistExecutionService.ReserveBehavioralSnapshotRequest(
                        88L, "PAGE", 1)))
        .hasMessageContaining("Cota diária segura");
  }

  /** Rejeita callback de pesquisa que tente concluir sem o contrato estratégico v2. */
  @Test
  void rejectsResearchCompletionWithoutMarketStrategicContract() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(7L);
    ExperimentStrategistExecution execution = execution(12L, plan);
    when(repository.findById(12L)).thenReturn(Optional.of(execution));
    ExperimentStrategistExecutionService service =
        new ExperimentStrategistExecutionService(
            repository,
            mock(ExperimentStrategistBehavioralSnapshotRepository.class),
            mock(CommercialPlanService.class),
            mock(ExperimentStrategistContextService.class),
            new ObjectMapper());

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                service.complete(
                    12L,
                    new ExperimentStrategistExecutionService.CompleteRequest(
                        "[{},{},{}]",
                        "{\"recommendation\":{}}",
                        "[{\"url\":\"a\"},{\"url\":\"b\"}]",
                        "{}",
                        "gpt-5.6-sol",
                        null)))
        .hasMessageContaining("Contrato Estratégico de Mercado v2 inválido");
  }

  /** Conclui Atena somente quando duas classes independentes sustentam o contrato v2. */
  @Test
  void completesResearchWithIndependentEvidenceClasses() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(7L);
    ExperimentStrategistExecution execution = execution(12L, plan);
    when(repository.findById(12L)).thenReturn(Optional.of(execution));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    ExperimentStrategistExecutionService service = service(repository);

    var result = service.complete(12L, validCompletionRequest("CUSTOMER_LANGUAGE", "PAID_OFFER"));

    assertThat(result.status()).isEqualTo(ExperimentStrategistExecutionStatus.COMPLETED);
    verify(repository).save(execution);
  }

  /** Rejeita fontes duplicadas que aparentariam corroborar a estratégia sem independência real. */
  @Test
  void rejectsResearchWithoutIndependentEvidenceClasses() {
    ExperimentStrategistExecutionRepository repository =
        mock(ExperimentStrategistExecutionRepository.class);
    CommercialPlan plan = new CommercialPlan();
    plan.setId(7L);
    ExperimentStrategistExecution execution = execution(12L, plan);
    when(repository.findById(12L)).thenReturn(Optional.of(execution));
    ExperimentStrategistExecutionService service = service(repository);

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> service.complete(12L, validCompletionRequest("PAID_OFFER", "PAID_OFFER")))
        .hasMessageContaining("Contrato Estratégico de Mercado v2 inválido");
  }

  /** Monta o serviço isolado usado pelos contratos de conclusão estratégica. */
  private ExperimentStrategistExecutionService service(
      ExperimentStrategistExecutionRepository repository) {
    return new ExperimentStrategistExecutionService(
        repository,
        mock(ExperimentStrategistBehavioralSnapshotRepository.class),
        mock(CommercialPlanService.class),
        mock(ExperimentStrategistContextService.class),
        new ObjectMapper());
  }

  /** Monta um callback v2 mínimo com as classes de evidência explicitamente informadas. */
  private ExperimentStrategistExecutionService.CompleteRequest validCompletionRequest(
      String firstEvidenceClass, String secondEvidenceClass) {
    return new ExperimentStrategistExecutionService.CompleteRequest(
        "[{},{},{}]",
        "{\"marketStrategicContract\":{\"contractVersion\":\"MARKET_STRATEGY_V2\","
            + "\"status\":\"READY_FOR_OPERATION\",\"evidenceReferences\":[\"source-1\",\"source-2\"],"
            + "\"operatorBoundary\":\"ATENA_DEFINES_STRATEGY_HERMES_OPERATES_GROWTH\"}}",
        "[{\"url\":\"https://source-1.example\",\"evidenceClass\":\""
            + firstEvidenceClass
            + "\"},{\"url\":\"https://source-2.example\",\"evidenceClass\":\""
            + secondEvidenceClass
            + "\"}]",
        "{}",
        "gpt-5.6-sol",
        null);
  }

  /** Cria uma execução mínima para cenários de lease. */
  private ExperimentStrategistExecution execution(Long id, CommercialPlan plan) {
    ExperimentStrategistExecution value = new ExperimentStrategistExecution();
    value.setId(id);
    value.setCommercialPlan(plan);
    value.setStatus(ExperimentStrategistExecutionStatus.RUNNING);
    value.setAuthorityMode("READ_ONLY_RESEARCH");
    value.setResearchQuestion("Qual oferta testar?");
    value.setEvidenceSnapshot("{}");
    value.setCreatedAt(Instant.now().minusSeconds(700));
    return value;
  }
}
