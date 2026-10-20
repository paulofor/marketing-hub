package com.marketinghub.agenttask;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Responsabilidade: selecionar o resultado vigente de cada responsável na passagem entre agentes.
 */
final class AgentTaskContextSelector {

  /** Impede instanciação do seletor puro, que não altera tarefas, gates ou custos. */
  private AgentTaskContextSelector() {}

  /**
   * Seleciona a última conclusão ou reprovação da mesma origem, atividade e agente; iniciar ou
   * cancelar uma retentativa não comprova resolução do resultado anterior.
   */
  static List<AgentTask> currentOutcomes(List<AgentTask> history, String sourceReference) {
    Map<ActivityOwner, AgentTask> latest = new LinkedHashMap<>();
    for (AgentTask task : history) {
      if (!Objects.equals(sourceReference, task.getSourceReference())
          || !("COMPLETED".equals(task.getStatus()) || "BLOCKED".equals(task.getStatus())))
        continue;
      var owner =
          new ActivityOwner(task.getProcessActivityId(), task.getAssignedAgent().getAgentKey());
      latest.merge(
          owner,
          task,
          (current, candidate) -> candidate.getId() > current.getId() ? candidate : current);
    }
    return latest.values().stream().sorted(Comparator.comparing(AgentTask::getId)).toList();
  }

  /**
   * Identifica responsabilidades independentes, inclusive em atividades históricas compartilhadas.
   */
  private record ActivityOwner(String activityId, String agentKey) {}
}
