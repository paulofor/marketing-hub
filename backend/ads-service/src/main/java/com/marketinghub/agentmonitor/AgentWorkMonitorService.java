package com.marketinghub.agentmonitor;

import com.marketinghub.agent.Agent;
import com.marketinghub.agenttask.AgentTask;
import com.marketinghub.creative.Creative;
import com.marketinghub.creative.CreativeAgentReviewStatus;
import com.marketinghub.creative.CreativeImprovementStatus;
import com.marketinghub.geralanding.GeraLandingStageExecution;
import com.marketinghub.opportunitydossier.OpportunityAgentReview;
import com.marketinghub.opportunitydossier.OpportunityReviewExecutionStatus;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.codextelemetry.CodexAgentExecutionTelemetryRepository;
import com.marketinghub.repository.jpa.creative.CreativeRepository;
import com.marketinghub.repository.jpa.geralanding.GeraLandingStageExecutionRepository;
import com.marketinghub.repository.jpa.opportunitydossier.OpportunityAgentReviewRepository;
import com.marketinghub.repository.jpa.salesvideo.VideoProductionCycleRepository;
import com.marketinghub.salesvideo.VideoProductionCycle;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
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
  private static final String TEMIS = "meta-ad-approver";
  private static final String ATENA = "experiment-strategist";
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
  private final CreativeRepository creativeRepository;
  private final AgentExecutorHealthService executorHealthService;
  private final OpportunityAgentReviewRepository opportunityReviews;

  /** Configura as fontes persistidas usadas pelo monitor. */
  @Autowired
  public AgentWorkMonitorService(
      AgentRepository agentRepository,
      AgentTaskRepository taskRepository,
      GeraLandingStageExecutionRepository landingRepository,
      VideoProductionCycleRepository videoCycleRepository,
      CodexAgentExecutionTelemetryRepository telemetryRepository,
      CreativeRepository creativeRepository,
      AgentExecutorHealthService executorHealthService,
      OpportunityAgentReviewRepository opportunityReviews) {
    this.agentRepository = agentRepository;
    this.taskRepository = taskRepository;
    this.landingRepository = landingRepository;
    this.videoCycleRepository = videoCycleRepository;
    this.telemetryRepository = telemetryRepository;
    this.creativeRepository = creativeRepository;
    this.executorHealthService = executorHealthService;
    this.opportunityReviews = opportunityReviews;
  }

  /**
   * Mantém testes unitários antigos focados somente no trabalho, sem persistência de health-check.
   */
  AgentWorkMonitorService(
      AgentRepository agentRepository,
      AgentTaskRepository taskRepository,
      GeraLandingStageExecutionRepository landingRepository,
      VideoProductionCycleRepository videoCycleRepository,
      CodexAgentExecutionTelemetryRepository telemetryRepository,
      CreativeRepository creativeRepository) {
    this(
        agentRepository,
        taskRepository,
        landingRepository,
        videoCycleRepository,
        telemetryRepository,
        creativeRepository,
        null,
        null);
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

  /** Solicita ao executor a reconexão Codex sem manipular tokens no backend. */
  public CodexAuthReconnectResponse requestReconnect(Long agentId, String requestedBy) {
    return executorHealthService.requestReconnect(agentId, requestedBy);
  }

  /** Recupera o estado auditável da reconexão para a tela administrativa. */
  public CodexAuthReconnectResponse currentReconnect(Long agentId) {
    return executorHealthService.currentReconnect(agentId);
  }

  /** Solicita atualização ou reinício auditável do executor selecionado. */
  public AgentExecutorAdminOperationResponse requestExecutorOperation(
      Long agentId, String operationType, String requestedBy) {
    return executorHealthService.requestOperation(agentId, operationType, requestedBy);
  }

  /** Recupera o último comando administrativo apresentado no painel. */
  public AgentExecutorAdminOperationResponse currentExecutorOperation(Long agentId) {
    return executorHealthService.currentOperation(agentId);
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
    if (ATENA.equals(agent.getAgentKey())) return atena(agent, tokens);
    if (APOLO.equals(agent.getAgentKey()) || PLUTUS.equals(agent.getAgentKey())) {
      return video(agent, tokens);
    }
    return task(agent, tokens);
  }

  /** Prioriza o parecer canônico mais recente de Atena sobre tarefas administrativas auxiliares. */
  private AgentWorkMonitorResponse atena(Agent agent, DailyTokenSnapshot tokens) {
    if (opportunityReviews == null) return task(agent, tokens);
    return opportunityReviews
        .findTopByAgentKeyOrderByUpdatedAtDescIdDesc("ATENA")
        .map(review -> atena(agent, review, tokens))
        .orElseGet(() -> task(agent, tokens));
  }

  /**
   * Cruza a execução do parecer com o health sem converter prontidão técnica em sucesso funcional.
   */
  private AgentWorkMonitorResponse atena(
      Agent agent, OpportunityAgentReview review, DailyTokenSnapshot tokens) {
    OpportunityReviewExecutionStatus status = review.getExecutionStatus();
    boolean failed = status == OpportunityReviewExecutionStatus.FAILED;
    boolean running = status == OpportunityReviewExecutionStatus.RUNNING;
    boolean completed = status == OpportunityReviewExecutionStatus.COMPLETED;
    return response(
        agent,
        failed ? "BLOCKED" : running ? "WORKING" : completed ? "COMPLETED" : "WAITING",
        "Parecer de Atena no dossiê #" + review.getDossier().getId(),
        "Execução canônica " + status,
        failed
            ? operationalBlocker(
                agent, review.getErrorMessage(), "Parecer bloqueado sem erro detalhado.")
            : null,
        false,
        null,
        "opportunity-dossier:" + review.getDossier().getId(),
        null,
        review.getId(),
        review.getUpdatedAt(),
        tokens);
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
            ? operationalBlocker(
                agent, execution.getErrorMessage(), "Falha técnica registrada na execução.")
            : stale ? "Execução sem atividade além da janela operacional." : null,
        false,
        null,
        "geralanding-experiment:" + execution.getExperimentId(),
        null,
        null,
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
        blocked ? operationalBlocker(agent, cycle.getFinancialReason(), "Ciclo bloqueado.") : null,
        financialPending,
        financialPending
            ? "Plutus precisa aprovar ou rejeitar o teto antes de qualquer provider."
            : null,
        "video-production-cycle:" + cycle.getId(),
        cycle.getAgentTaskId(),
        cycle.getId(),
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
          agent,
          "IDLE",
          "Sem trabalho ativo",
          null,
          null,
          false,
          null,
          null,
          null,
          null,
          null,
          tokens);
    }
    return task(agent, history.getFirst(), tokens);
  }

  /** Traduz uma solicitação da caixa de entrada para o monitor. */
  private AgentWorkMonitorResponse task(Agent agent, AgentTask task, DailyTokenSnapshot tokens) {
    if (TEMIS.equals(agent.getAgentKey())) {
      AgentWorkMonitorResponse creativeWork = temisCreativeWork(agent, task, tokens);
      if (creativeWork != null) return creativeWork;
    }
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
        task.getId(),
        null,
        task.getUpdatedAt(),
        tokens);
  }

  /** Prioriza a execução criativa real da Têmis sobre o status histórico da tarefa agregadora. */
  private AgentWorkMonitorResponse temisCreativeWork(
      Agent agent, AgentTask task, DailyTokenSnapshot tokens) {
    Long experimentId = experimentId(task.getSourceReference());
    if (experimentId == null) return null;
    return creativeRepository.findTemisOpenExecutions(experimentId).stream()
        .findFirst()
        .map(creative -> temisCreativeWork(agent, task, creative, tokens))
        .orElse(null);
  }

  /**
   * Traduz revisão e materialização da imagem mais recente em tarefa, execução e bloqueio atuais.
   */
  private AgentWorkMonitorResponse temisCreativeWork(
      Agent agent, AgentTask task, Creative creative, DailyTokenSnapshot tokens) {
    CreativeAgentReviewStatus review = creative.getAgentReviewStatus();
    CreativeImprovementStatus improvement = creative.getAgentImprovementStatus();
    boolean improving =
        improvement == CreativeImprovementStatus.PENDING
            || improvement == CreativeImprovementStatus.PROCESSING;
    boolean reviewing =
        review == CreativeAgentReviewStatus.PENDING
            || review == CreativeAgentReviewStatus.PROCESSING;
    boolean blocked =
        improvement == CreativeImprovementStatus.FAILED
            || improvement == CreativeImprovementStatus.LIMIT_REACHED
            || review == CreativeAgentReviewStatus.ADJUST
            || review == CreativeAgentReviewStatus.REJECTED
            || review == CreativeAgentReviewStatus.FAILED;
    if (!improving && !reviewing && !blocked && !"BLOCKED".equals(task.getStatus())) return null;
    String phase =
        improving
            ? "Produzindo e enviando uma imagem melhor"
            : reviewing ? "Revisando o anúncio" : "Correção visual bloqueada";
    String difficulty =
        blocked
            ? operationalBlocker(
                agent, temisBlockReason(creative), "A correção do criativo está bloqueada.")
            : null;
    return response(
        agent,
        blocked ? "BLOCKED" : "WORKING",
        "Tarefa #" + task.getId() + " — " + task.getTitle(),
        phase + " · criativo #" + creative.getId(),
        difficulty,
        false,
        null,
        task.getSourceReference(),
        task.getId(),
        creative.getId(),
        creative.getAgentReviewedAt() != null ? creative.getAgentReviewedAt() : task.getUpdatedAt(),
        tokens);
  }

  /** Expõe a causa persistida mais específica sem substituir por uma mensagem genérica. */
  private String temisBlockReason(Creative creative) {
    if (creative.getAgentImprovementError() != null
        && !creative.getAgentImprovementError().isBlank()) {
      return creative.getAgentImprovementError();
    }
    if (creative.getRejectionReason() != null && !creative.getRejectionReason().isBlank()) {
      return creative.getRejectionReason();
    }
    return "O criativo #"
        + creative.getId()
        + " aguarda uma nova imagem que cumpra o parecer visual.";
  }

  /** Extrai o experimento apenas da referência canônica usada pela tarefa. */
  private Long experimentId(String sourceReference) {
    if (sourceReference == null || !sourceReference.matches("experiment:\\d+")) return null;
    return Long.valueOf(sourceReference.substring("experiment:".length()));
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
      Long taskId,
      Long executionId,
      Instant lastActivity,
      DailyTokenSnapshot tokens) {
    AgentExecutorHealthResponse executorHealth =
        executorHealthService == null
            ? AgentExecutorHealthResponse.unknown(agent.getCurrentVersion())
            : executorHealthService.current(agent);
    String combinedStatus =
        ATENA.equals(agent.getAgentKey())
                && "READY".equals(executorHealth.status())
                && "BLOCKED".equals(status)
                && source != null
                && source.startsWith("opportunity-dossier:")
            ? "READY — parecer bloqueado"
            : executorHealth.status();
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
        taskId,
        executionId,
        lastActivity,
        dailyTokens(agent, tokens),
        tokens.date(),
        executorHealth,
        combinedStatus);
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

  /** Converte detalhes técnicos persistidos em um bloqueio curto e acionável para o painel. */
  private String operationalBlocker(Agent agent, String value, String fallback) {
    String error = text(value, fallback).trim();
    if (isAuthenticationError(error)) {
      return "Autenticação do executor inválida ou expirada. Reconecte a credencial usada por "
          + agent.getNickname()
          + " e retome a tarefa.";
    }
    return conciseError(error);
  }

  /** Reconhece os sinais comuns de credencial inválida sem depender de um único provedor. */
  private boolean isAuthenticationError(String error) {
    String normalized = error.toLowerCase(Locale.ROOT);
    return normalized.contains("refresh_token_reused")
        || normalized.contains("invalid_grant")
        || normalized.contains("token expired")
        || normalized.contains("token expirado")
        || normalized.contains("unauthorized")
        || normalized.contains("authentication failed")
        || normalized.contains("invalid token")
        || normalized.contains("invalid_token")
        || normalized.contains("http 401")
        || normalized.contains("status 401")
        || normalized.contains("401 unauthorized");
  }

  /** Remove stack trace e limita o resumo exibido sem alterar a evidência persistida. */
  private String conciseError(String error) {
    int end = error.length();
    for (String marker : List.of("\n", "\r", "\tat ", " Caused by:", " at com.")) {
      int markerIndex = error.indexOf(marker);
      if (markerIndex >= 0) end = Math.min(end, markerIndex);
    }
    String summary = error.substring(0, end).trim();
    int maxLength = 280;
    return summary.length() <= maxLength ? summary : summary.substring(0, maxLength - 1) + "…";
  }
}
