package com.marketinghub.agentmonitor;

import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: consolidar tarefas e pipelines persistidos em um monitor operacional por
 * agente.
 */
@Service
public class AgentWorkMonitorService {
  private static final Duration DEDALO_STALE_AFTER = Duration.ofMinutes(45);
  private static final String DEDALO = "landing-generator";
  private static final String APOLO = "videomaker";
  private static final String PLUTUS = "financial-agent";
  private final AgentRepository agentRepository;
  private final AgentTaskRepository taskRepository;
  private final GeraLandingStageExecutionRepository landingRepository;
  private final VideoProductionCycleRepository videoCycleRepository;

  /** Configura as fontes persistidas usadas pelo monitor. */
  public AgentWorkMonitorService(
      AgentRepository agentRepository,
      AgentTaskRepository taskRepository,
      GeraLandingStageExecutionRepository landingRepository,
      VideoProductionCycleRepository videoCycleRepository) {
    this.agentRepository = agentRepository;
    this.taskRepository = taskRepository;
    this.landingRepository = landingRepository;
    this.videoCycleRepository = videoCycleRepository;
  }

  /** Lista todos os agentes, inclusive os ociosos, com bloqueios e decisões externas explícitos. */
  @Transactional(readOnly = true)
  public List<AgentWorkMonitorResponse> list() {
    return agentRepository.findAllByOrderByNicknameAsc().stream().map(this::monitor).toList();
  }

  /** Resolve a fonte operacional mais relevante para a identidade do agente. */
  private AgentWorkMonitorResponse monitor(Agent agent) {
    if (DEDALO.equals(agent.getAgentKey())) return dedalo(agent);
    if (APOLO.equals(agent.getAgentKey()) || PLUTUS.equals(agent.getAgentKey())) {
      return video(agent);
    }
    return task(agent);
  }

  /** Consolida a execução autônoma mais recente de Dédalo. */
  private AgentWorkMonitorResponse dedalo(Agent agent) {
    return landingRepository
        .findTopByStageCodeOrderByExecutionRequestedAtDesc("landing-generation-agent-v1")
        .map(execution -> landing(agent, execution))
        .orElseGet(() -> task(agent));
  }

  /** Traduz o estado persistido do GeraLanding sem inferir sucesso comercial. */
  private AgentWorkMonitorResponse landing(Agent agent, GeraLandingStageExecution execution) {
    String status = normalize(execution.getStatus());
    boolean failed =
        status.equals("FAILED")
            || status.equals("FALHOU")
            || status.equals("FALHA")
            || status.equals("ERRO");
    boolean done =
        status.equals("COMPLETED") || status.equals("CONCLUIDO") || status.equals("CONCLUÍDO");
    Instant lastActivity = activity(execution);
    boolean stale =
        !failed
            && !done
            && lastActivity != null
            && lastActivity.plus(DEDALO_STALE_AFTER).isBefore(Instant.now());
    return response(
        agent,
        failed || stale ? "BLOCKED" : done ? "COMPLETED" : "WORKING",
        "Correção autônoma da landing do experimento #" + execution.getExperimentId(),
        "Etapa landing-generation-agent-v1 · " + execution.getStatus(),
        failed
            ? text(execution.getErrorMessage(), "Falha técnica registrada na execução.")
            : stale ? "Execução sem atividade além da janela operacional." : null,
        false,
        null,
        "geralanding-experiment:" + execution.getExperimentId(),
        lastActivity);
  }

  /** Consolida o mesmo ciclo de vídeo sob as perspectivas de Apolo e Plutus. */
  private AgentWorkMonitorResponse video(Agent agent) {
    return videoCycleRepository
        .findTopByOrderByUpdatedAtDesc()
        .map(cycle -> video(agent, cycle))
        .orElseGet(() -> task(agent));
  }

  /** Traduz o ciclo financeiro e criativo em trabalho, bloqueio ou decisão. */
  private AgentWorkMonitorResponse video(Agent agent, VideoProductionCycle cycle) {
    boolean plutus = PLUTUS.equals(agent.getAgentKey());
    String status = cycle.getStatus();
    boolean financialPending = "PENDING_FINANCIAL_REVIEW".equals(status);
    boolean blocked = "FINANCIAL_BLOCKED".equals(status) || "FAILED".equals(status);
    String work =
        plutus
            ? "Controle financeiro do ciclo de vídeo #" + cycle.getId()
            : "Produção audiovisual do ciclo #" + cycle.getId();
    String workStatus =
        blocked
            ? "BLOCKED"
            : financialPending && plutus
                ? "DECISION_REQUIRED"
                : financialPending ? "WAITING" : "WORKING";
    return response(
        agent,
        workStatus,
        work,
        "Teto US$ "
            + cycle.getBudgetLimitUsd()
            + " · custo conhecido US$ "
            + cycle.getKnownCostUsd(),
        blocked ? text(cycle.getFinancialReason(), "Ciclo bloqueado.") : null,
        financialPending,
        financialPending
            ? "Plutus precisa aprovar ou rejeitar o teto antes de qualquer provider."
            : null,
        "video-production-cycle:" + cycle.getId(),
        cycle.getUpdatedAt());
  }

  /** Usa a tarefa aberta mais recente como fallback para qualquer agente. */
  private AgentWorkMonitorResponse task(Agent agent) {
    List<AgentTask> history =
        taskRepository.findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc(agent.getAgentKey());
    if (history.isEmpty()
        || !List.of("PENDING", "IN_PROGRESS", "BLOCKED").contains(history.getFirst().getStatus())) {
      return response(agent, "IDLE", "Sem trabalho ativo", null, null, false, null, null, null);
    }
    return task(agent, history.getFirst());
  }

  /** Traduz uma solicitação da caixa de entrada para o monitor. */
  private AgentWorkMonitorResponse task(Agent agent, AgentTask task) {
    boolean blocked = "BLOCKED".equals(task.getStatus());
    boolean gate =
        "GATE_DECISION".equals(task.getTaskKind()) && "PENDING".equals(task.getGateStatus());
    boolean pending = "PENDING".equals(task.getStatus());
    return response(
        agent,
        gate ? "DECISION_REQUIRED" : blocked ? "BLOCKED" : pending ? "WAITING" : "WORKING",
        task.getTitle(),
        task.getDescription(),
        blocked ? "Tarefa marcada como bloqueada." : null,
        gate,
        gate ? "Decisão externa pendente no gate " + task.getGateCode() + "." : null,
        task.getSourceReference(),
        task.getUpdatedAt());
  }

  /** Monta o contrato uniforme do monitor. */
  private AgentWorkMonitorResponse response(
      Agent agent,
      String status,
      String work,
      String detail,
      String difficulty,
      boolean decisionRequired,
      String decision,
      String source,
      Instant lastActivity) {
    return new AgentWorkMonitorResponse(
        agent.getId(),
        agent.getAgentKey(),
        agent.getNickname(),
        agent.getName(),
        status,
        work,
        detail,
        difficulty,
        decisionRequired,
        decision,
        source,
        lastActivity);
  }

  /** Seleciona a data mais recente disponível na execução. */
  private Instant activity(GeraLandingStageExecution execution) {
    if (execution.getCompletedAt() != null) return execution.getCompletedAt();
    if (execution.getProcessingStartedAt() != null) return execution.getProcessingStartedAt();
    return execution.getExecutionRequestedAt();
  }

  /** Normaliza estados técnicos para comparação sem perder o valor exibido. */
  private String normalize(String value) {
    return value == null ? "" : value.trim().toUpperCase();
  }

  /** Usa o fallback somente quando o detalhe persistido estiver vazio. */
  private String text(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
