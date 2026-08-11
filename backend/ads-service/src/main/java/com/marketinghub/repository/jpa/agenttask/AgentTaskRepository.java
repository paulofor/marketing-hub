package com.marketinghub.repository.jpa.agenttask;

import com.marketinghub.agenttask.AgentTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar as caixas de entrada dos agentes. */
public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {
  /** Lista a caixa do agente com as solicitações mais recentes primeiro. */
  List<AgentTask> findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc(String agentKey);
}
