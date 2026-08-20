package com.marketinghub.agentmonitor;

import com.marketinghub.agent.Agent;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.agentmonitor.AgentAutomaticExecutionControlEventRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: consultar e alterar o controle operacional PLAY/STOP dos agentes. */
@Service
public class AgentAutomaticExecutionControlService {
  private final AgentRepository agents;
  private final AgentAutomaticExecutionControlEventRepository events;
  private final Clock clock;

  /** Configura a fonte atual, a trilha auditável e o relógio operacional. */
  @Autowired
  public AgentAutomaticExecutionControlService(
      AgentRepository agents, AgentAutomaticExecutionControlEventRepository events) {
    this(agents, events, Clock.systemUTC());
  }

  /** Permite testes determinísticos da decisão e da data persistidas. */
  AgentAutomaticExecutionControlService(
      AgentRepository agents, AgentAutomaticExecutionControlEventRepository events, Clock clock) {
    this.agents = agents;
    this.events = events;
    this.clock = clock;
  }

  /** Altera o estado somente quando necessário e registra a mudança em trilha append-only. */
  @Transactional
  public AgentAutomaticExecutionControlResponse update(
      Long agentId, boolean automaticExecutionEnabled, String changedBy) {
    Agent agent =
        agents.findLockedById(agentId).orElseThrow(() -> notFound("Agente não encontrado."));
    boolean current = enabled(agent);
    if (current == automaticExecutionEnabled) return response(agent);

    Instant changedAt = clock.instant();
    String operator = normalizeOperator(changedBy);
    agent.setAutomaticExecutionEnabled(automaticExecutionEnabled);
    agent.setAutomaticExecutionChangedAt(changedAt);
    agent.setAutomaticExecutionChangedBy(operator);
    agents.save(agent);
    events.save(
        new AgentAutomaticExecutionControlEvent(
            agent, automaticExecutionEnabled, operator, changedAt));
    return response(agent);
  }

  /** Retorna o estado operacional pelo identificador técnico usado pelo executor. */
  @Transactional(readOnly = true)
  public AgentAutomaticExecutionControlResponse current(String agentKey) {
    Agent agent =
        agents
            .findByAgentKey(agentKey)
            .orElseThrow(() -> notFound("Agente técnico não encontrado."));
    return response(agent);
  }

  /** Converte o cadastro persistido no contrato uniforme PLAY/STOP. */
  public AgentAutomaticExecutionControlResponse response(Agent agent) {
    boolean executionEnabled = enabled(agent);
    return new AgentAutomaticExecutionControlResponse(
        agent.getId(),
        agent.getAgentKey(),
        executionEnabled,
        executionEnabled ? "PLAY" : "STOP",
        agent.getAutomaticExecutionChangedAt(),
        agent.getAutomaticExecutionChangedBy());
  }

  /** Mantém PLAY como compatibilidade segura para cadastros anteriores à migração. */
  private boolean enabled(Agent agent) {
    return !Boolean.FALSE.equals(agent.getAutomaticExecutionEnabled());
  }

  /** Limita a autoria administrativa sem aceitar valores vazios ou excessivos. */
  private String normalizeOperator(String changedBy) {
    String operator =
        changedBy == null || changedBy.isBlank() ? "marketing-hub-admin" : changedBy.trim();
    return operator.length() <= 100 ? operator : operator.substring(0, 100);
  }

  /** Produz uma resposta HTTP adequada sem transformar ausência de cadastro em erro interno. */
  private ResponseStatusException notFound(String message) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
  }
}
