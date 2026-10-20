package com.marketinghub.agenttask;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.agent.Agent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: impedir contradições e mistura de responsáveis no contexto compartilhado. */
class AgentTaskContextSelectorTest {

  /** Retentar ou cancelar não resolve uma reprovação ainda válida do mesmo agente. */
  @ParameterizedTest
  @ValueSource(strings = {"PENDING", "IN_PROGRESS", "CANCELLED"})
  void preservesUnresolvedOutcomeDuringRetry(String status) {
    var blocked = task(10L, "experiment:91", "review", "customer-agent", "BLOCKED");
    var retry = task(11L, "experiment:91", "review", "customer-agent", status);
    assertThat(AgentTaskContextSelector.currentOutcomes(List.of(retry, blocked), "experiment:91"))
        .containsExactly(blocked);
  }

  /**
   * Um revisor não encerra a pendência independente de outro em atividade histórica compartilhada.
   */
  @Test
  void preservesIndependentResponsibilities() {
    var psique = task(10L, "experiment:91", "review", "customer-agent", "BLOCKED");
    var themis = task(11L, "experiment:91", "review", "meta-ad-approver", "BLOCKED");
    var accepted = task(12L, "experiment:91", "review", "customer-agent", "COMPLETED");
    assertThat(
            AgentTaskContextSelector.currentOutcomes(
                List.of(accepted, themis, psique), "experiment:91"))
        .containsExactly(themis, accepted);
  }

  /** Outra origem não pode resolver bloqueio nem fornecer aprovação ao contexto solicitado. */
  @ParameterizedTest
  @ValueSource(strings = {"experiment:92", "experiment:91@video-proof-v1", "product:91"})
  void isolatesExactSource(String foreignSource) {
    var blocked = task(10L, "experiment:91", "review", "customer-agent", "BLOCKED");
    var foreign = task(20L, foreignSource, "review", "customer-agent", "COMPLETED");
    assertThat(AgentTaskContextSelector.currentOutcomes(List.of(blocked, foreign), "experiment:91"))
        .containsExactly(blocked);
  }

  /** A ordenação recebida e horários iguais não podem reativar um resultado anterior. */
  @Test
  void resolvesChronologyWithoutDependingOnRepositoryOrder() {
    var old = task(10L, "experiment:91", "review", "customer-agent", "COMPLETED");
    var blocked = task(11L, "experiment:91", "review", "customer-agent", "BLOCKED");
    var current = task(12L, "experiment:91", "review", "customer-agent", "COMPLETED");
    var independent = task(13L, "experiment:91", "safety", "customer-agent", "BLOCKED");
    assertThat(
            AgentTaskContextSelector.currentOutcomes(
                List.of(current, old, independent, blocked), "experiment:91"))
        .containsExactly(current, independent);
  }

  /**
   * Mantém predecessores válidos e histórico vazio sem inventar conclusões ou bloquear execução.
   */
  @Test
  void preservesPreviouslyValidContext() {
    var delivery = task(10L, "experiment:91", "delivery", "landing-generator", "COMPLETED");
    var review = task(11L, "experiment:91", "review", "customer-agent", "COMPLETED");
    assertThat(AgentTaskContextSelector.currentOutcomes(List.of(delivery, review), "experiment:91"))
        .containsExactly(delivery, review);
    assertThat(AgentTaskContextSelector.currentOutcomes(List.of(), "experiment:91")).isEmpty();
  }

  /** Cria evidência sintética sem banco, inferência, autorização comercial ou dado de cliente. */
  private AgentTask task(long id, String source, String activity, String owner, String status) {
    var agent = new Agent();
    agent.setAgentKey(owner);
    var task = new AgentTask();
    task.setId(id);
    task.setAssignedAgent(agent);
    task.setSourceReference(source);
    task.setProcessActivityId(activity);
    task.setStatus(status);
    task.setCreatedAt(Instant.parse("2026-10-20T00:00:00Z"));
    return task;
  }
}
