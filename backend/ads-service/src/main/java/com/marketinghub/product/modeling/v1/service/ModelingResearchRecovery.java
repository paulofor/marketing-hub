package com.marketinghub.product.modeling.v1.service;

import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.product.modeling.v1.service.get.ModelingResearchRecoveryView;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.service.ProductDiscoveryBpmAuditService;
import com.marketinghub.repository.jpa.agent.CodexAuthReconnectRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Retoma somente pesquisa inicial recusada por autenticação, com reconexão posterior comprovada.
 */
@Service
@RequiredArgsConstructor
public class ModelingResearchRecovery {
  private final ProductDiscoveryCycleRepository cycles;
  private final AgentTaskRepository tasks;
  private final CodexAuthReconnectRepository reconnections;
  private final ProductDiscoveryBpmAuditService audit;

  /** Consulta evidências persistidas sem reabrir fila, tarefa ou sessão. */
  public ModelingResearchRecoveryView readiness(ProductDiscoveryCycle cycle) {
    if (cycle == null
        || cycle.getStatus() != ProductDiscoveryCycleStatus.FAILED
        || !"research".equals(cycle.getStageCode()))
      return blocked("Esta pesquisa não está interrompida na coleta inicial.");
    if (StringUtils.hasText(cycle.getResearchPlanJson())
        || StringUtils.hasText(cycle.getResearchPlanModel())
        || StringUtils.hasText(cycle.getResearchPlanRawResponse())
        || StringUtils.hasText(cycle.getResearchAnalysisRawResponse())
        || StringUtils.hasText(cycle.getResearchEvidenceReportJson()))
      return blocked(
          "Há execução ou resultado preservado; confira a recuperação antes de repetir IA.");
    List<AgentTask> attempts =
        tasks
            .findBySourceReferenceOrderByCreatedAtAscIdAsc(
                "product-discovery-cycle:" + cycle.getId())
            .stream()
            .filter(t -> t.getAssignedAgent() != null)
            .filter(t -> "market-radar".equals(t.getAssignedAgent().getAgentKey()))
            .filter(t -> t.getProcessDefinition() != null)
            .filter(
                t -> "pde-opportunity-discovery".equals(t.getProcessDefinition().getProcessCode()))
            .toList();
    if (attempts.stream().anyMatch(t -> List.of("PENDING", "IN_PROGRESS").contains(t.getStatus())))
      return blocked("Argos já tem uma tentativa ativa desta pesquisa.");
    var last = attempts.stream().max(Comparator.comparing(AgentTask::getId)).orElse(null);
    if (last == null
        || !"BLOCKED".equals(last.getStatus())
        || !List.of("marketEvidence", "inspiration", "evidence")
            .contains(last.getProcessActivityId())
        || last.getUpdatedAt() == null
        || !authenticationFailure(last.getExecutionError())
        || positive(last.getInputTokens())
        || positive(last.getCachedInputTokens())
        || positive(last.getOutputTokens())
        || (last.getEstimatedCostUsd() != null && last.getEstimatedCostUsd().signum() > 0))
      return blocked("A falha não comprova recusa de autenticação anterior à pesquisa.");
    var connection =
        reconnections
            .findTopByAgentIdOrderByRequestedAtDesc(last.getAssignedAgent().getId())
            .orElse(null);
    if (connection == null
        || !"AUTHENTICATED".equals(connection.getStatus())
        || connection.getCompletedAt() == null
        || !connection.getCompletedAt().isAfter(last.getUpdatedAt()))
      return blocked("Reconecte Argos após a falha para liberar a retomada desta pesquisa.");
    return new ModelingResearchRecoveryView(
        true,
        "Reconexão de Argos confirmada. A tentativa anterior não produziu plano nem dossiê; a pesquisa pode ser retomada dentro do saldo autorizado.",
        connection.getId());
  }

  /** Reabre a mesma fila atomicamente e preserva a tentativa bloqueada e os limites existentes. */
  @Transactional
  public void resume(Long cycleId) {
    var cycle = cycles.findByIdForUpdate(cycleId).orElseThrow();
    if (cycle.getStatus() == ProductDiscoveryCycleStatus.READY_FOR_RESEARCH
        || cycle.getStatus() == ProductDiscoveryCycleStatus.RESEARCHING) return;
    var readiness = readiness(cycle);
    if (!readiness.available())
      throw new ResponseStatusException(HttpStatus.CONFLICT, readiness.reason());
    String reason =
        "Pesquisa retomada explicitamente após reconexão Codex #"
            + readiness.reconnectionId()
            + ". Tentativa anterior preservada; sem plano ou dossiê produzido. Autorizações e limites não foram alterados.";
    audit.reopenInitialResearch(cycle, reason);
    cycle.setStatus(ProductDiscoveryCycleStatus.READY_FOR_RESEARCH);
    cycle.setErrorMessage(null);
    cycle.setExecutionLeaseId(null);
    cycle.setLeaseExpiresAt(null);
    cycle.setDecisionSummary(reason);
    cycles.save(cycle);
  }

  /**
   * Reconhece somente a recusa explícita de acesso, sem classificar falha de conteúdo como login.
   */
  private boolean authenticationFailure(String error) {
    if (error == null) return false;
    String text = error.toLowerCase(Locale.ROOT);
    return text.contains("unauthorized") && text.matches("(?s).*\\b401\\b.*");
  }

  /** Identifica uso informado sem converter ausência de custo em gratuidade. */
  private boolean positive(Long value) {
    return value != null && value > 0;
  }

  /** Mantém explícito o motivo pelo qual uma nova tentativa não pode ser aberta. */
  private ModelingResearchRecoveryView blocked(String reason) {
    return new ModelingResearchRecoveryView(false, reason, null);
  }
}
