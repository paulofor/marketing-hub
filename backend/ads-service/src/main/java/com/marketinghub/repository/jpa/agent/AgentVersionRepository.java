package com.marketinghub.repository.jpa.agent;

import com.marketinghub.agent.AgentVersion;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Responsabilidade: persistir o historico imutavel dos contratos dos agentes. */
public interface AgentVersionRepository extends JpaRepository<AgentVersion, Long> {
  /** Lista versoes do agente da mais recente para a mais antiga. */
  List<AgentVersion> findByAgentIdOrderByVersionNumberDesc(Long agentId);

  /** Recupera em lote a data da versao que governa atualmente cada agente. */
  @Query(
      """
      select av.agent.id as agentId, av.createdAt as changedAt
      from AgentVersion av
      where av.agent.id in :agentIds
        and av.versionNumber = av.agent.currentVersion
      """)
  List<CurrentVersionChange> findCurrentVersionChanges(@Param("agentIds") List<Long> agentIds);

  /** Projeta somente a identidade do agente e a data auditavel de sua versao atual. */
  interface CurrentVersionChange {
    /** Informa o agente ao qual a versao pertence. */
    Long getAgentId();

    /** Informa quando a versao atual do contrato foi criada. */
    Instant getChangedAt();
  }
}
