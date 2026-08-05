package com.marketinghub.repository.jpa.agent;

import com.marketinghub.agent.AgentVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Responsabilidade: persistir o historico imutavel dos contratos dos agentes. */
public interface AgentVersionRepository extends JpaRepository<AgentVersion, Long> {
  /** Lista versoes do agente da mais recente para a mais antiga. */
  List<AgentVersion> findByAgentIdOrderByVersionNumberDesc(Long agentId);
}
