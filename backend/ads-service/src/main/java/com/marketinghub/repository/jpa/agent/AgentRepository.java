package com.marketinghub.repository.jpa.agent;

import com.marketinghub.agent.Agent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de Agent.
 */
public interface AgentRepository extends JpaRepository<Agent, Long> {

    Optional<Agent> findDetailedById(Long id);

    List<Agent> findAllByOrderByNameAsc();
}
