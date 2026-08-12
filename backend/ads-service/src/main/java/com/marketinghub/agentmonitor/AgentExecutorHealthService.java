package com.marketinghub.agentmonitor;

import com.marketinghub.agent.Agent;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Responsabilidade: registrar e classificar a prontidão operacional dos executores dos agentes. */
@Service
public class AgentExecutorHealthService {
  private static final Duration MAX_AGE = Duration.ofMinutes(10);
  private final AgentRepository agents;
  private final AgentExecutorHealthCheckRepository checks;
  private final Clock clock;

  /** Configura as fontes canônicas de agente, leitura operacional e tempo. */
  public AgentExecutorHealthService(
      AgentRepository agents, AgentExecutorHealthCheckRepository checks) {
    this(agents, checks, Clock.systemUTC());
  }

  /** Permite validar vencimento de leituras com relógio determinístico. */
  AgentExecutorHealthService(
      AgentRepository agents, AgentExecutorHealthCheckRepository checks, Clock clock) {
    this.agents = agents;
    this.checks = checks;
    this.clock = clock;
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
