package com.marketinghub.repository.jpa.agenttask;

import com.marketinghub.agenttask.AgentTask;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir e consultar as caixas de entrada dos agentes. */
public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {
  /** Lista a caixa do agente com as solicitações mais recentes primeiro. */
  List<AgentTask> findByAssignedAgentAgentKeyOrderByCreatedAtDescIdDesc(String agentKey);

  /** Lista os registros vinculados a qualquer versão de um plano comercial. */
  List<AgentTask> findBySourceReferenceStartingWithOrderByUpdatedAtDescIdDesc(
      String sourceReferencePrefix);

  /** Busca a tarefa mais recente pela referência exata, sem confundir ids com prefixo comum. */
  Optional<AgentTask> findTopBySourceReferenceOrderByUpdatedAtDescIdDesc(String sourceReference);

  /** Busca a delegação mais recente de um agente para uma origem operacional exata. */
  Optional<AgentTask> findTopByAssignedAgentAgentKeyAndSourceReferenceOrderByUpdatedAtDescIdDesc(
      String agentKey, String sourceReference);

  /** Lista tarefas operacionais da Têmis nos estados que ainda exigem execução. */
  List<AgentTask> findByAssignedAgentAgentKeyAndTaskKindAndStatusInOrderByCreatedAtAscIdAsc(
      String agentKey, String taskKind, List<String> statuses);
}
