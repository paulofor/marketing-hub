package com.marketinghub.agentmonitor;

import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.codextelemetry.CodexAgentExecutionTelemetryRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
  private static final Map<String, String> TELEMETRY_TYPE_BY_AGENT_KEY =
      Map.of(
          "customer-agent", "CUSTOMER_AGENT",
          "financial-agent", "FINANCIAL_AGENT",
          "growth-operator", "GROWTH_OPERATOR",
          "experiment-strategist", "EXPERIMENT_STRATEGIST",
          "meta-ad-approver", "META_AD_APPROVER",
          "landing-generator", "LANDING_GENERATOR");
  private final AgentRepository agentRepository;
  private final AgentTaskRepository taskRepository;
  private final GeraLandingStageExecutionRepository landingRepository;
  private final VideoProductionCycleRepository videoCycleRepository;
  private final CodexAgentExecutionTelemetryRepository telemetryRepository;

  /** Configura as fontes persistidas usadas pelo monitor. */
  public AgentWorkMonitorService(
      AgentRepository agentRepository,
      AgentTaskRepository taskRepository,
      GeraLandingStageExecutionRepository landingRepository,
      VideoProductionCycleRepository videoCycleRepository,
      CodexAgentExecutionTelemetryRepository telemetryRepository) {
    this.agentRepository = agentRepository;
    this.taskRepository = taskRepository;
    this.landingRepository = landingRepository;
    this.videoCycleRepository = videoCycleRepository;
    this.telemetryRepository = telemetryRepository;
  }

  /** Lista todos os agentes, inclusive os ociosos, com bloqueios e decisões externas explícitos. */
  @Transactional(readOnly = true)
  public List<AgentWorkMonitorResponse> list() {
    LocalDate date = LocalDate.now(BUSINESS_ZONE);
    Instant start = date.atStartOfDay(BUSINESS_ZONE).toInstant();
    Instant end = date.plusDays(1).atStartOfDay(BUSINESS_ZONE).toInstant();
    DailyTokenSnapshot tokens =
        new DailyTokenSnapshot(
            date, tokenTotals(telemetryRepository.sumTokensByAgentTypeBetween(start, end)));
    return agentRepository.findAllByOrderByNicknameAsc().stream()
        .map(agent -> monitor(agent, tokens))
        .toList();
  }

  /** Converte a agregação persistida em totais acessíveis pela identidade da telemetria. */
  private Map<String, Long> tokenTotals(List<Object[]> rows) {
    Map<String, Long> totals = new HashMap<>();
    for (Object[] row : rows) {
      totals.put((String) row[0], ((Number) row[1]).longValue());
    }
    return totals;
  }

  /** Resolve a fonte operacional mais relevante para a identidade do agente. */
  private AgentWorkMonitorResponse monitor(Agent agent, DailyTokenSnapshot tokens) {
    if (DEDALO.equals(agent.getAgentKey())) return dedalo(agent, tokens);
    if (APOLO.equals(agent.getAgentKey()) || PLUTUS.equals(agent.getAgentKey())) {
      return video(agent, tokens);
    }
    return task(agent, tokens);
  }

  /** Consolida a execução autônoma mais recente de Dédalo. */
  private AgentWorkMonitorResponse dedalo(Agent agent, DailyTokenSnapshot tokens) {
    return landingRepository
        .findTopByStageCodeOrderByExecutionRequestedAtDesc("landing-generation-agent-v1")
        .map(execution -> landing(agent, execution, tokens))
        .orElseGet(() -> task(agent, tokens));
  }

  /** Traduz o estado persistido do GeraLanding sem inferir sucesso comercial. */
  private AgentWorkMonitorResponse landing(
      Agent agent, GeraLandingStageExecution execution, DailyTokenSnapshot tokens) {
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
        lastActivity,
        tokens);
  }

  /** Consolida o mesmo ciclo de vídeo sob as perspectivas de Apolo e Plutus. */
  private AgentWorkMonitorResponse video(Agent agent, DailyTokenSnapshot tokens) {
    return videoCycleRepository
        .findTopByOrderByUpdatedAtDesc()
        .map(cycle -> video(agent, cycle, tokens))
        .orElseGet(() -> task(agent, tokens));
  }

  /** Traduz o ciclo financeiro e criativo em trabalho, bloqueio ou decisão. */
  private AgentWorkMonitorResponse video(
      Agent agent, VideoProductionCycle cycle, DailyTokenSnapshot tokens) {
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
        cycle.getUpdatedAt(),
        tokens);
  }

  /** Usa a tarefa aberta mais recente como fallback para qualquer agente. */
  private AgentWorkMonitorResponse task(Agent agent, DailyTokenSnapshot tokens) {
    List<AgentTask> history =
        taskRepository.findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc(agent.getAgentKey());
    if (history.isEmpty()
        || !List.of("PENDING", "IN_PROGRESS", "BLOCKED").contains(history.getFirst().getStatus())) {
      return response(
          agent, "IDLE", "Sem trabalho ativo", null, null, false, null, null, null, tokens);
    }
    return task(agent, history.getFirst(), tokens);
  }

  /** Traduz uma solicitação da caixa de entrada para o monitor. */
  private AgentWorkMonitorResponse task(Agent agent, AgentTask task, DailyTokenSnapshot tokens) {
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
        task.getUpdatedAt(),
        tokens);
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
      Instant lastActivity,
      DailyTokenSnapshot tokens) {
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
        lastActivity,
        dailyTokens(agent, tokens),
        tokens.date());
  }

  /** Retorna o consumo diário comprovado ou zero quando o agente ainda não reportou tokens. */
  private long dailyTokens(Agent agent, DailyTokenSnapshot tokens) {
    String telemetryType = TELEMETRY_TYPE_BY_AGENT_KEY.get(agent.getAgentKey());
    return telemetryType == null ? 0L : tokens.byAgentType().getOrDefault(telemetryType, 0L);
  }

  /** Mantém a data e os totais da mesma leitura isolados por requisição. */
  private record DailyTokenSnapshot(LocalDate date, Map<String, Long> byAgentType) {}

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
