package com.marketinghub.agentmonitor;

import com.marketinghub.agent.Agent;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agent.CodexAuthReconnectRepository;
import com.marketinghub.repository.jpa.agentmonitor.AgentExecutorAdminOperationRepository;
import com.marketinghub.repository.jpa.agentmonitor.AgentExecutorHealthCheckRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: registrar e classificar a prontidão operacional dos executores dos agentes. */
@Service
public class AgentExecutorHealthService {
  private static final Duration MAX_AGE = Duration.ofMinutes(10);
  private static final Set<String> CODEX_EXECUTORS =
      Set.of(
          "customer-agent",
          "financial-agent",
          "growth-operator",
          "experiment-strategist",
          "communication-director",
          "meta-ad-approver",
          "landing-generator",
          "videomaker",
          "market-radar");
  private final AgentRepository agents;
  private final AgentExecutorHealthCheckRepository checks;
  private final Clock clock;
  private final CodexAuthReconnectRepository reconnects;
  private final AgentExecutorAdminOperationRepository operations;

  /** Configura as fontes canônicas de agente, leitura operacional e tempo. */
  @Autowired
  public AgentExecutorHealthService(
      AgentRepository agents,
      AgentExecutorHealthCheckRepository checks,
      CodexAuthReconnectRepository reconnects,
      AgentExecutorAdminOperationRepository operations) {
    this(agents, checks, reconnects, operations, Clock.systemUTC());
  }

  /** Mantém testes focados no health-check sem persistência de reconexão. */
  AgentExecutorHealthService(AgentRepository agents, AgentExecutorHealthCheckRepository checks) {
    this(agents, checks, null, null, Clock.systemUTC());
  }

  /** Mantém testes de vencimento com relógio determinístico. */
  AgentExecutorHealthService(
      AgentRepository agents, AgentExecutorHealthCheckRepository checks, Clock clock) {
    this(agents, checks, null, null, clock);
  }

  /** Mantém testes de reconexão focados sem persistência de comandos administrativos. */
  AgentExecutorHealthService(
      AgentRepository agents,
      AgentExecutorHealthCheckRepository checks,
      CodexAuthReconnectRepository reconnects) {
    this(agents, checks, reconnects, null, Clock.systemUTC());
  }

  /** Permite validar vencimento de leituras com relógio determinístico. */
  AgentExecutorHealthService(
      AgentRepository agents,
      AgentExecutorHealthCheckRepository checks,
      CodexAuthReconnectRepository reconnects,
      Clock clock) {
    this(agents, checks, reconnects, null, clock);
  }

  /** Permite validar comandos administrativos com relógio determinístico. */
  AgentExecutorHealthService(
      AgentRepository agents,
      AgentExecutorHealthCheckRepository checks,
      CodexAuthReconnectRepository reconnects,
      AgentExecutorAdminOperationRepository operations,
      Clock clock) {
    this.agents = agents;
    this.checks = checks;
    this.clock = clock;
    this.reconnects = reconnects;
    this.operations = operations;
  }

  /** Solicita atualização ou reinício sem conceder acesso Docker ao backend. */
  @Transactional
  public AgentExecutorAdminOperationResponse requestOperation(
      Long agentId, String operationType, String requestedBy) {
    Agent agent =
        agents
            .findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agente não encontrado."));
    if (!CODEX_EXECUTORS.contains(agent.getAgentKey()))
      throw new IllegalStateException("Agente não utiliza executor Codex administrável.");
    String normalized = operationType == null ? "" : operationType.trim().toUpperCase();
    if (!Set.of("UPDATE", "RESTART").contains(normalized))
      throw new IllegalArgumentException("Operação deve ser UPDATE ou RESTART.");
    if (operations.existsByAgentIdAndStatusIn(agentId, java.util.List.of("REQUESTED", "RUNNING")))
      return currentOperation(agentId);
    return operationResponse(
        operations.save(
            new AgentExecutorAdminOperation(
                agent, normalized, concise(requestedBy, 100), clock.instant())));
  }

  /** Consulta o último comando administrativo do agente. */
  @Transactional(readOnly = true)
  public AgentExecutorAdminOperationResponse currentOperation(Long agentId) {
    return operations
        .findTopByAgentIdOrderByRequestedAtDesc(agentId)
        .map(this::operationResponse)
        .orElse(null);
  }

  /** Reserva o próximo comando para o controlador externo do host. */
  @Transactional
  public AgentExecutorAdminOperationResponse claimOperation() {
    return operations
        .findTopByStatusOrderByRequestedAtAsc("REQUESTED")
        .map(
            item -> {
              item.start(clock.instant());
              return operationResponse(operations.save(item));
            })
        .orElse(null);
  }

  /** Finaliza o comando; a prontidão seguirá dependente do health-check real. */
  @Transactional
  public AgentExecutorAdminOperationResponse completeOperation(
      Long id, AgentExecutorAdminCompletionRequest request) {
    AgentExecutorAdminOperation item =
        operations
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Operação não encontrada."));
    item.complete(request.success(), concise(request.detail(), 500), clock.instant());
    return operationResponse(operations.save(item));
  }

  /** Converte a operação persistida no contrato administrativo. */
  private AgentExecutorAdminOperationResponse operationResponse(AgentExecutorAdminOperation item) {
    return new AgentExecutorAdminOperationResponse(
        item.getId(),
        item.getAgent().getId(),
        item.getAgent().getAgentKey(),
        item.getOperationType(),
        item.getStatus(),
        item.getRequestedBy(),
        item.getRequestedAt(),
        item.getStartedAt(),
        item.getCompletedAt(),
        item.getDetail());
  }

  /** Solicita reconexão sem permitir duas operações concorrentes para o mesmo agente. */
  @Transactional
  public CodexAuthReconnectResponse requestReconnect(Long agentId, String requestedBy) {
    Agent agent =
        agents
            .findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agente não encontrado."));
    if (agent.getAgentKey() == null || agent.getAgentKey().isBlank())
      throw new IllegalStateException("Agente não possui executor técnico configurado.");
    if (!CODEX_EXECUTORS.contains(agent.getAgentKey()))
      throw new IllegalStateException("Agente não utiliza executor Codex com sessão individual.");
    if (reconnects.existsByAgentIdAndStatusIn(
        agentId, java.util.List.of("REQUESTED", "STARTING", "AWAITING_CONFIRMATION"))) {
      CodexAuthReconnectResponse current = currentReconnect(agentId);
      if (current.requestedAt().isAfter(clock.instant().minus(Duration.ofMinutes(20))))
        return current;
      CodexAuthReconnect stale = reconnects.findById(current.id()).orElseThrow();
      stale.finish(false, "Solicitação anterior expirou antes da confirmação.", clock.instant());
      reconnects.save(stale);
    }
    CodexAuthReconnect saved =
        reconnects.save(new CodexAuthReconnect(agent, concise(requestedBy, 100), clock.instant()));
    return reconnectResponse(saved);
  }

  /** Consulta a última reconexão conhecida pelo backend. */
  @Transactional(readOnly = true)
  public CodexAuthReconnectResponse currentReconnect(Long agentId) {
    return reconnects
        .findTopByAgentIdOrderByRequestedAtDesc(agentId)
        .map(this::reconnectResponse)
        .orElse(null);
  }

  /** Reserva a próxima solicitação pelo identificador técnico do executor. */
  @Transactional
  public CodexAuthReconnectResponse claimReconnect(String agentKey) {
    return reconnects
        .findTop1ByAgentAgentKeyAndStatusOrderByRequestedAtAsc(agentKey, "REQUESTED")
        .stream()
        .findFirst()
        .map(
            item -> {
              item.start(clock.instant());
              return reconnectResponse(reconnects.save(item));
            })
        .orElse(null);
  }

  /** Registra o device code temporário sem aceitar credenciais. */
  @Transactional
  public CodexAuthReconnectResponse deviceCode(Long id, CodexAuthDeviceCodeRequest request) {
    CodexAuthReconnect item =
        reconnects
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reconexão não encontrada."));
    item.awaitConfirmation(
        concise(request.verificationUrl(), 500), concise(request.userCode(), 30));
    return reconnectResponse(reconnects.save(item));
  }

  /** Finaliza a operação após o executor validar account/read. */
  @Transactional
  public CodexAuthReconnectResponse complete(Long id, CodexAuthCompletionRequest request) {
    CodexAuthReconnect item =
        reconnects
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Reconexão não encontrada."));
    item.finish(request.authenticated(), concise(request.detail(), 500), clock.instant());
    return reconnectResponse(reconnects.save(item));
  }

  /** Converte a entidade auditável no contrato seguro da API. */
  private CodexAuthReconnectResponse reconnectResponse(CodexAuthReconnect item) {
    return new CodexAuthReconnectResponse(
        item.getId(),
        item.getAgent().getId(),
        item.getAgent().getAgentKey(),
        item.getStatus(),
        item.getVerificationUrl(),
        item.getUserCode(),
        item.getRequestedBy(),
        item.getDetail(),
        item.getRequestedAt(),
        item.getStartedAt(),
        item.getCompletedAt());
  }

  /** Persiste a prova recebida e calcula o estado sem confiar no status do remetente. */
  @Transactional
  public AgentExecutorHealthResponse report(AgentExecutorHealthReportRequest request) {
    Agent agent =
        agents
            .findByAgentKey(request.agentKey())
            .orElseThrow(() -> new IllegalArgumentException("Agente técnico não encontrado."));
    boolean versionCurrent = agent.getCurrentVersion().equals(request.deployedVersion());
    String status =
        versionCurrent && request.backendAccessible() && request.codexAuthenticated()
            ? "READY"
            : "BLOCKED";
    AgentExecutorHealthCheck check =
        checks.save(
            new AgentExecutorHealthCheck(
                agent,
                request.deployedVersion(),
                concise(request.buildReference(), 100),
                request.backendAccessible(),
                request.codexAuthenticated(),
                status,
                concise(request.detail(), 500),
                clock.instant()));
    return response(agent, check);
  }

  /** Recupera a leitura vigente e invalida automaticamente provas antigas. */
  @Transactional(readOnly = true)
  public AgentExecutorHealthResponse current(Agent agent) {
    return checks
        .findTopByAgentAgentKeyOrderByCheckedAtDesc(agent.getAgentKey())
        .map(check -> response(agent, check))
        .orElseGet(() -> AgentExecutorHealthResponse.unknown(agent.getCurrentVersion()));
  }

  /** Converte a leitura persistida em diagnóstico de versão, rede e autenticação. */
  private AgentExecutorHealthResponse response(Agent agent, AgentExecutorHealthCheck check) {
    boolean versionCurrent = agent.getCurrentVersion().equals(check.getDeployedVersion());
    boolean stale = check.getCheckedAt().isBefore(clock.instant().minus(MAX_AGE));
    String status = stale ? "UNKNOWN" : check.getStatus();
    String detail =
        stale ? "Verificação vencida; o executor deve repetir o health-check." : check.getDetail();
    return new AgentExecutorHealthResponse(
        status,
        agent.getCurrentVersion(),
        check.getDeployedVersion(),
        versionCurrent,
        check.isBackendAccessible(),
        check.isCodexAuthenticated(),
        check.getBuildReference(),
        detail,
        check.getCheckedAt());
  }

  /** Limita dados diagnósticos antes da persistência administrativa. */
  private String concise(String value, int limit) {
    if (value == null || value.isBlank()) return null;
    String singleLine = value.replaceAll("[\\r\\n]+", " ").trim();
    return singleLine.length() <= limit ? singleLine : singleLine.substring(0, limit - 1) + "…";
  }
}
