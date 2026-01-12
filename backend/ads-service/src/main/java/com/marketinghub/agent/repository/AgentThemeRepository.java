package com.marketinghub.agent.repository;

import com.marketinghub.agent.AgentTheme;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentThemeRepository extends JpaRepository<AgentTheme, Long> {
    List<AgentTheme> findAllByOrderByNameAsc();
}
