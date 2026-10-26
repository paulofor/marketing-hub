package com.marketinghub.product.modeling.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.marketinghub.agent.Agent;
import com.marketinghub.agentmonitor.CodexAuthReconnect;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryBpmAuditService;
import com.marketinghub.repository.jpa.agent.CodexAuthReconnectRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Reproduz a recusa de acesso de Vega e protege recuperação, isolamento e consumo anterior. */
class ModelingResearchRecoveryTest {
  final ProductDiscoveryCycleRepository cycles = mock(ProductDiscoveryCycleRepository.class);
  final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  final CodexAuthReconnectRepository connections = mock(CodexAuthReconnectRepository.class);
  final ProductDiscoveryBpmAuditService audit = mock(ProductDiscoveryBpmAuditService.class);
  final ModelingResearchRecovery service =
      new ModelingResearchRecovery(cycles, tasks, connections, audit);
  final Instant failedAt = Instant.parse("2026-10-26T13:35:16Z");
  ProductDiscoveryCycle cycle;
  AgentTask failed;
  CodexAuthReconnect connection;

  /** Prepara a falha histórica sem inferência concluída e uma sessão autenticada posteriormente. */
  @BeforeEach
  void setup() {
    prepare(73L, 5L);
  }

  /** Cria identidades sintéticas distintas sem credenciais ou chamada externa. */
  private void prepare(long cycleId, long agentId) {
    cycle = new ProductDiscoveryCycle();
    cycle.setId(cycleId);
    cycle.setStatus(ProductDiscoveryCycleStatus.FAILED);
    cycle.setStageCode("research");
    cycle.setErrorMessage("workspace routing discovery unauthorized (401)");
    cycle.setCommercialConstraints("Teto cumulativo preservado; mídia não autorizada.");
    var agent = new Agent();
    agent.setId(agentId);
    agent.setAgentKey("market-radar");
    var process = new BusinessProcessDefinition();
    process.setId(115L);
    process.setProcessCode("pde-opportunity-discovery");
    failed = new AgentTask();
    failed.setId(cycleId + 1000);
    failed.setAssignedAgent(agent);
    failed.setProcessDefinition(process);
    failed.setProcessActivityId("marketEvidence");
    failed.setStatus("BLOCKED");
    failed.setExecutionError(cycle.getErrorMessage());
    failed.setExecutionMode("MODEL");
    failed.setUpdatedAt(failedAt);
    connection = mock(CodexAuthReconnect.class);
    when(connection.getId()).thenReturn(25L);
    when(connection.getStatus()).thenReturn("AUTHENTICATED");
    when(connection.getCompletedAt()).thenReturn(failedAt.plusSeconds(300));
    when(connections.findTopByAgentIdOrderByRequestedAtDesc(agentId))
        .thenReturn(Optional.of(connection));
    when(cycles.findByIdForUpdate(cycleId)).thenReturn(Optional.of(cycle));
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:" + cycleId))
        .thenReturn(List.of(failed));
  }

  /** O caso original e outra identidade voltam à mesma fila sem alterar histórico ou orçamento. */
  @ParameterizedTest
  @ValueSource(longs = {73L, 99173L})
  void resumesOnlyAfterNewAuthentication(long id) {
    prepare(id, id + 500);
    assertThat(service.readiness(cycle).available()).isTrue();
    service.resume(id);
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    assertThat(cycle.getDecisionSummary()).contains("#25", "limites não foram alterados");
    assertThat(cycle.getCommercialConstraints())
        .isEqualTo("Teto cumulativo preservado; mídia não autorizada.");
    assertThat(failed.getStatus()).isEqualTo("BLOCKED");
    assertThat(failed.getExecutionError()).contains("401");
    assertThat(failed.getEstimatedCostUsd()).isNull();
    verify(audit).reopenInitialResearch(eq(cycle), contains("#25"));
    service.resume(id);
    verify(audit, times(1)).reopenInitialResearch(any(), any());
  }

  /** Recupera a falha de contrato de coleta sem apagar o plano pago ou ampliar o orçamento. */
  @ParameterizedTest
  @ValueSource(longs = {73L, 99173L})
  void reusesPlanAfterCollectionContractFailure(long id) {
    prepare(id, id + 500);
    String plan = "{\"researchLens\":\"Plano preservado\"}";
    cycle.setResearchPlanJson(plan);
    cycle.setResearchPlanRawResponse(plan);
    cycle.setResearchPlanModel("gpt-6.1-sol");
    failed.setInputTokens(19901L);
    failed.setOutputTokens(2358L);
    failed.setExecutionError(
        "Backend recusou HTTP 409 /meta-ad-browser-collection: ciclo B2C orientado ao Instagram");
    assertThat(service.readiness(cycle).available()).isTrue();
    service.resume(id);
    assertThat(cycle.getResearchPlanJson()).isEqualTo(plan);
    assertThat(cycle.getResearchPlanRawResponse()).isEqualTo(plan);
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    verify(audit).estimatePersistedUsage(failed.getId());
    verify(audit).reopenInitialResearch(eq(cycle), contains("plano original preservado"));
  }

  /** Uma conexão antiga ou ainda pendente não comprova que a causa foi corrigida. */
  @Test
  void rejectsOldOrPendingAuthentication() {
    when(connection.getCompletedAt()).thenReturn(failedAt.minusSeconds(1));
    assertThat(service.readiness(cycle).available()).isFalse();
    when(connection.getCompletedAt()).thenReturn(failedAt.plusSeconds(300));
    when(connection.getStatus()).thenReturn("AWAITING_CONFIRMATION");
    assertThatThrownBy(() -> service.resume(73L)).hasMessageContaining("Reconecte Argos");
    verifyNoInteractions(audit);
  }

  /** Dados de plano, resposta ou custo informado impedem repetir consumo por essa recuperação. */
  @Test
  void refusesPreservedOutputAndReportedUsage() {
    cycle.setResearchPlanRawResponse("resposta preservada");
    assertThat(service.readiness(cycle).available()).isFalse();
    cycle.setResearchPlanRawResponse(null);
    cycle.setResearchAnalysisRawResponse("dossiê preservado");
    assertThat(service.readiness(cycle).available()).isFalse();
    cycle.setResearchAnalysisRawResponse(null);
    failed.setInputTokens(1L);
    assertThat(service.readiness(cycle).available()).isFalse();
    failed.setInputTokens(null);
    failed.setEstimatedCostUsd(new BigDecimal("0.10"));
    assertThatThrownBy(() -> service.resume(73L)).hasMessageContaining("não comprova");
    verifyNoInteractions(audit);
  }

  /** Falhas de conteúdo e aprofundamentos conservam seus contratos próprios de recuperação. */
  @Test
  void rejectsOtherFailureAndOtherStage() {
    failed.setExecutionError("Contrato de pesquisa inválido");
    assertThat(service.readiness(cycle).available()).isFalse();
    failed.setExecutionError("unauthorized (401)");
    cycle.setStageCode("candidate-gap-deepening");
    assertThat(service.readiness(cycle).available()).isFalse();
    verifyNoInteractions(audit);
  }

  /** Trabalho ativo e agentes de outra identidade não autorizam uma tentativa duplicada. */
  @Test
  void rejectsActiveAndForeignAttempts() {
    var active = new AgentTask();
    active.setId(2000L);
    active.setAssignedAgent(failed.getAssignedAgent());
    active.setProcessDefinition(failed.getProcessDefinition());
    active.setStatus("IN_PROGRESS");
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:73"))
        .thenReturn(List.of(failed, active));
    assertThat(service.readiness(cycle).available()).isFalse();
    when(tasks.findBySourceReferenceOrderByCreatedAtAscIdAsc("product-discovery-cycle:73"))
        .thenReturn(List.of(failed));
    failed.getAssignedAgent().setAgentKey("experiment-strategist");
    assertThat(service.readiness(cycle).available()).isFalse();
    verifyNoInteractions(audit);
  }

  /** Leitura, rascunho e pesquisa em andamento não criam tarefas nem alteram estados. */
  @Test
  void preservesPreviouslyValidPathsAndReadOnlyQueries() {
    assertThat(service.readiness(cycle).available()).isTrue();
    verifyNoInteractions(audit);
    cycle.setStatus(ProductDiscoveryCycleStatus.DRAFT);
    assertThat(service.readiness(cycle).available()).isFalse();
    cycle.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    service.resume(73L);
    assertThat(cycle.getStatus()).isEqualTo(ProductDiscoveryCycleStatus.RESEARCHING);
    verifyNoInteractions(audit);
  }
}
