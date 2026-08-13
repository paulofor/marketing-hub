package com.marketinghub.repository.jpa.agentmonitor;

import com.marketinghub.agentmonitor.AgentExecutorHealthCheck;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: recuperar a prova operacional mais recente de cada executor. */
public interface AgentExecutorHealthCheckRepository
    extends JpaRepository<AgentExecutorHealthCheck, Long> {
  /** Localiza a leitura mais recente de um agente técnico. */
  Optional<AgentExecutorHealthCheck> findTopByAgentAgentKeyOrderByCheckedAtDesc(String agentKey);
}
