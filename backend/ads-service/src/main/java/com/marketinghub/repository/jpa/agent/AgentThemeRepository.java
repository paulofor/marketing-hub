package com.marketinghub.repository.jpa.agent;

import com.marketinghub.agent.AgentTheme;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositório JPA responsável pela persistência de AgentTheme.
 */
public interface AgentThemeRepository extends JpaRepository<AgentTheme, Long> {
    List<AgentTheme> findAllByOrderByNameAsc();
}
