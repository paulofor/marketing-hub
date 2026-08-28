package com.marketinghub.agentmemory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marketinghub.agentmemory.PremiumAgentMemory;
import com.marketinghub.repository.jpa.agentmemory.PremiumAgentMemoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Responsabilidade: validar a consolidação auditável do aprendizado dos agentes. */
@ExtendWith(MockitoExtension.class)
class AgentLearningDashboardServiceTest {
  @Mock private PremiumAgentMemoryRepository repository;

  /** Confirma que estados e reutilizações são agregados sem inventar resultados. */
  @Test
  void aggregatesPersistedEvidenceByAgent() {
    PremiumAgentMemory confirmed = memory("apollo", "CONFIRMED", 3);
    PremiumAgentMemory candidate = memory("apollo", "CANDIDATE", 2);
    when(repository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(confirmed, candidate));

    var response = new AgentLearningDashboardService(repository).dashboard();

    assertThat(response.totalMemories()).isEqualTo(2);
    assertThat(response.confirmedMemories()).isEqualTo(1);
    assertThat(response.candidateMemories()).isEqualTo(1);
    assertThat(response.totalRetrievals()).isEqualTo(5);
    assertThat(response.agents())
        .filteredOn(agent -> agent.agentKey().equals("apollo"))
        .singleElement()
        .satisfies(
            agent -> {
              assertThat(agent.agentName()).isEqualTo("Apolo");
              assertThat(agent.totalRetrievals()).isEqualTo(5);
            });
    assertThat(response.agents())
        .filteredOn(agent -> agent.agentKey().equals("communication-director"))
        .singleElement()
        .satisfies(
            agent -> {
              assertThat(agent.agentName()).isEqualTo("Íris");
              assertThat(agent.totalMemories()).isZero();
            });
  }

  /** Cria uma memória completa para testar somente a consolidação. */
  private PremiumAgentMemory memory(String agentKey, String status, long retrievals) {
    PremiumAgentMemory value = new PremiumAgentMemory();
    value.setAgentKey(agentKey);
    value.setTenantKey("tenant-1");
    value.setScopeType("PROJECT");
    value.setScopeId("15");
    value.setSpecialty("Roteiro");
    value.setContent("Demonstrar o produto antes da promessa.");
    value.setEvidence("Resultado observado no replay.");
    value.setSourceExecutionId("execution-15");
    value.setStatus(status);
    value.setConfidence(new BigDecimal("0.8000"));
    value.setRetrievalCount(retrievals);
    value.setCreatedAt(Instant.parse("2026-08-15T10:00:00Z"));
    value.setUpdatedAt(Instant.parse("2026-08-15T10:00:00Z"));
    return value;
  }
}
