package com.marketinghub.agent.integration;

import com.marketinghub.agent.Agent;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Responsabilidade: associar cada agente à recência auditável de seu executor no GitHub Actions.
 */
@Service
public class AgentWorkflowFreshnessService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AgentWorkflowFreshnessService.class);

  private final AgentWorkflowClient client;
  private final AgentWorkflowProperties properties;
  private volatile CachedRuns cachedRuns;

  /** Configura o cliente externo e a política de cache da leitura operacional. */
  public AgentWorkflowFreshnessService(
      AgentWorkflowClient client, AgentWorkflowProperties properties) {
    this.client = client;
    this.properties = properties;
  }

  /**
   * Retorna a última execução concluída de cada executor, sem bloquear a lista se o GitHub falhar.
   */
  @Transactional(readOnly = true)
  public Map<Long, AgentWorkflowFreshness> currentWorkflowRuns(List<Agent> agents) {
    Map<String, String> workflowsByAgentKey = properties.getWorkflowByAgentKey();
    if (agents.isEmpty() || workflowsByAgentKey == null || workflowsByAgentKey.isEmpty()) {
      return Map.of();
    }

    List<AgentWorkflowClient.WorkflowRun> runs = loadRunsSafely();
    Map<String, AgentWorkflowClient.WorkflowRun> latestByWorkflow =
        runs.stream()
            .filter(run -> Objects.equals(properties.getBranch(), run.branch()))
            .filter(run -> run.completedAt() != null)
            .collect(
                Collectors.toMap(
                    AgentWorkflowClient.WorkflowRun::workflowFile,
                    Function.identity(),
                    this::latestRun));

    return agents.stream()
        .filter(agent -> agent.getAgentKey() != null)
        .map(
            agent -> {
              String workflowFile = workflowsByAgentKey.get(agent.getAgentKey());
              AgentWorkflowClient.WorkflowRun run = latestByWorkflow.get(workflowFile);
              return run == null
                  ? null
                  : Map.entry(
                      agent.getId(),
                      new AgentWorkflowFreshness(
                          run.completedAt(),
                          run.workflowName(),
                          run.workflowFile(),
                          run.conclusion(),
                          run.url()));
            })
        .filter(Objects::nonNull)
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /** Retorna a execução mais recente do mesmo workflow pela data de conclusão. */
  private AgentWorkflowClient.WorkflowRun latestRun(
      AgentWorkflowClient.WorkflowRun first, AgentWorkflowClient.WorkflowRun second) {
    return Comparator.comparing(
                    AgentWorkflowClient.WorkflowRun::completedAt,
                    Comparator.nullsFirst(Comparator.naturalOrder()))
                .compare(first, second)
            >= 0
        ? first
        : second;
  }

  /** Consulta o GitHub com cache e converte indisponibilidade em ausência explícita de dado. */
  private List<AgentWorkflowClient.WorkflowRun> loadRunsSafely() {
    Instant now = Instant.now();
    CachedRuns current = cachedRuns;
    Duration cacheTtl = properties.getCacheTtl();
    if (current != null && cacheTtl != null && current.loadedAt().plus(cacheTtl).isAfter(now)) {
      return current.runs();
    }

    try {
      List<AgentWorkflowClient.WorkflowRun> runs =
          client.listCompletedRuns(properties.getRepository(), properties.getBranch());
      cachedRuns = new CachedRuns(now, runs);
      return runs;
    } catch (Exception ex) {
      LOGGER.warn(
          "Falha ao consultar recência dos workflows dos agentes no GitHub; contrato local será preservado",
          ex);
      return List.of();
    }
  }

  /** Mantém em memória a resposta externa por um intervalo curto e controlado. */
  private record CachedRuns(Instant loadedAt, List<AgentWorkflowClient.WorkflowRun> runs) {}
}
