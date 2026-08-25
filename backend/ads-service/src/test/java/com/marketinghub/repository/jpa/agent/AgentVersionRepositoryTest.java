package com.marketinghub.repository.jpa.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.agent.Agent;
import com.marketinghub.agent.AgentTheme;
import com.marketinghub.agent.AgentVersion;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/** Responsabilidade: comprovar a consulta da ultima alteracao contratual dos agentes. */
@DataJpaTest
@TestPropertySource(properties = "spring.liquibase.enabled=false")
class AgentVersionRepositoryTest {

  @Autowired private AgentVersionRepository repository;
  @Autowired private EntityManager entityManager;

  /** Retorna somente a data da versao que governa o agente no momento da consulta. */
  @Test
  void findsCurrentVersionChangeWithoutUsingOperationalAgentUpdate() {
    AgentTheme theme = new AgentTheme();
    theme.setName("Operações Autônomas");
    entityManager.persist(theme);

    Agent agent = new Agent();
    agent.setTheme(theme);
    agent.setName("Agente Videomaker");
    agent.setNickname("Apolo");
    agent.setAgentKey("videomaker");
    agent.setExecutionMode("EVENT_DRIVEN");
    agent.setCurrentVersion(2);
    entityManager.persist(agent);

    persistVersion(agent, 1, Instant.parse("2026-08-10T10:00:00Z"));
    Instant currentChangedAt = Instant.parse("2026-08-12T16:36:24Z");
    persistVersion(agent, 2, currentChangedAt);
    entityManager.flush();
    entityManager.clear();

    List<AgentVersionRepository.CurrentVersionChange> changes =
        repository.findCurrentVersionChanges(List.of(agent.getId()));

    assertThat(changes).hasSize(1);
    assertThat(changes.getFirst().getAgentId()).isEqualTo(agent.getId());
    assertThat(changes.getFirst().getChangedAt()).isEqualTo(currentChangedAt);
  }

  /** Persiste uma fotografia imutavel de contrato para o cenario de consulta. */
  private void persistVersion(Agent agent, int versionNumber, Instant createdAt) {
    AgentVersion version = new AgentVersion();
    version.setAgent(agent);
    version.setVersionNumber(versionNumber);
    version.setContractSnapshot("{\"version\":" + versionNumber + "}");
    version.setCreatedAt(createdAt);
    entityManager.persist(version);
  }
}
