package com.marketinghub.systemimprovement;

import com.marketinghub.agent.Agent;
import com.marketinghub.repository.jpa.agent.AgentRepository;
import com.marketinghub.repository.jpa.systemimprovement.SystemImprovementRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: centralizar o cadastro auditável de melhorias propostas por todos os agentes.
 */
@Service
public class SystemImprovementService {
  private final SystemImprovementRepository repository;
  private final AgentRepository agentRepository;
  private final Clock clock;

  /** Configura a persistência, o catálogo de agentes e o relógio de auditoria. */
  @Autowired
  public SystemImprovementService(
      SystemImprovementRepository repository, AgentRepository agentRepository) {
    this(repository, agentRepository, Clock.systemUTC());
  }

  /** Permite testes determinísticos sem alterar o relógio operacional. */
  SystemImprovementService(
      SystemImprovementRepository repository, AgentRepository agentRepository, Clock clock) {
    this.repository = repository;
    this.agentRepository = agentRepository;
    this.clock = clock;
  }

  /** Registra uma melhoria em nome de qualquer agente existente no catálogo. */
  @Transactional
  public SystemImprovementResponse create(CreateSystemImprovementRequest request) {
    Agent agent =
        agentRepository
            .findByAgentKey(request.agentKey().trim())
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Agente solicitante não encontrado."));
    SystemImprovement value = new SystemImprovement();
    value.setRequestedByAgent(agent);
    value.setTitle(request.title().trim());
    value.setDescription(request.description().trim());
    value.setTaskReference(trimToNull(request.taskReference()));
    value.setStatus("SUGGESTED");
    value.setRequestedAt(Instant.now(clock));
    return response(repository.save(value));
  }

  /** Lista o backlog de melhorias para consulta administrativa. */
  @Transactional(readOnly = true)
  public List<SystemImprovementResponse> list() {
    return repository.findAllByOrderByRequestedAtDescIdDesc().stream().map(this::response).toList();
  }

  /** Converte a entidade preservando a identidade humana e técnica do agente. */
  private SystemImprovementResponse response(SystemImprovement value) {
    Agent agent = value.getRequestedByAgent();
    return new SystemImprovementResponse(
        value.getId(),
        agent.getId(),
        agent.getAgentKey(),
        agent.getNickname(),
        value.getTitle(),
        value.getDescription(),
        value.getTaskReference(),
        value.getStatus(),
        value.getRequestedAt());
  }

  /** Normaliza referências opcionais sem persistir texto vazio. */
  private String trimToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
