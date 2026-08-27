package com.marketinghub.repository.jpa.agenttask;

import com.marketinghub.agenttask.AgentTaskActivityCoverage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir a cobertura adicional de atividades por uma tarefa composta. */
public interface AgentTaskActivityCoverageRepository
    extends JpaRepository<AgentTaskActivityCoverage, Long> {

  /** Confirma o vínculo idempotente entre a tarefa e a atividade versionada. */
  boolean existsByAgentTaskIdAndActivityDefinitionId(Long agentTaskId, Long activityDefinitionId);

  /** Lista as atividades adicionais cobertas por um conjunto conhecido de tarefas. */
  List<AgentTaskActivityCoverage> findAllByAgentTaskIdIn(List<Long> agentTaskIds);
}
