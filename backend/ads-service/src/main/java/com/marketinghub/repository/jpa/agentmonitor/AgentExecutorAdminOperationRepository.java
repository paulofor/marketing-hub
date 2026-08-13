package com.marketinghub.repository.jpa.agentmonitor;

import com.marketinghub.agentmonitor.AgentExecutorAdminOperation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir comandos administrativos destinados aos executores dos agentes. */
public interface AgentExecutorAdminOperationRepository
    extends JpaRepository<AgentExecutorAdminOperation, Long> {
  /** Localiza a solicitação mais antiga ainda disponível ao controlador. */
  Optional<AgentExecutorAdminOperation> findTopByStatusOrderByRequestedAtAsc(String status);

  /** Localiza a última operação apresentada no painel para o agente. */
  Optional<AgentExecutorAdminOperation> findTopByAgentIdOrderByRequestedAtDesc(Long agentId);

  /** Impede comandos concorrentes para o mesmo executor. */
  boolean existsByAgentIdAndStatusIn(Long agentId, java.util.Collection<String> statuses);
}
