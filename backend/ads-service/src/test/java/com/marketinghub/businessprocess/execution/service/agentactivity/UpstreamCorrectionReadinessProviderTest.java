package com.marketinghub.businessprocess.execution.service.agentactivity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: provar retorno causal, progresso funcional e limite que a retomada não zera.
 */
class UpstreamCorrectionReadinessProviderTest {
  private final ObjectMapper json = new ObjectMapper();
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final UpstreamCorrectionReadinessProvider gate =
      new UpstreamCorrectionReadinessProvider(cycles, tasks, json);
  private final List<AgentTaskFunctionalSnapshot> history = new ArrayList<>();
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private final Product product = Product.builder().id(99007L).build();
  private static final String REF = "experiment:99112";

  /** Monta um BPM sintético com gate humano e três responsabilidades independentes. */
  @BeforeEach
  void setup() {
    process.setId(99126L);
    process.setDiagramJson(
        """
        {"nodes":[
        {"id":"human","type":"TASK","executorType":"HUMAN","responsibleAgentKeys":["operator"]},
        {"id":"strategy","label":"Atena — estratégia","type":"TASK","responsibleAgentKeys":["atena"]},
        {"id":"economy","label":"Plutus — economia","type":"TASK","responsibleAgentKeys":["plutus"]},
        {"id":"architecture","label":"Dédalo — arquitetura","type":"TASK","responsibleAgentKeys":["dedalo"]}],
        "flows":[{"from":"human","to":"strategy"},{"from":"strategy","to":"economy"},{"from":"economy","to":"architecture"}]}
        """);
    cycle.setId(99119L);
    cycle.setProductId(product.getId());
    cycle.setExperimentId(99112L);
    cycle.setStatus("OPEN");
    cycle.setCreatedAt(Instant.parse("2026-10-30T00:00:00Z"));
    when(cycles.findByExperimentId(99112L)).thenReturn(Optional.of(cycle));
    when(tasks.findFunctionalSnapshotsByProcessSince(process.getId(), REF, cycle.getCreatedAt()))
        .thenAnswer(ignored -> List.copyOf(history));
    add(1, "strategy", "COMPLETED", approved("entrada antiga"));
    add(2, "economy", "COMPLETED", approved("economia inicial"));
  }

  /** Retorna à estratégia e revalida somente a cadeia causal antes da arquitetura. */
  @Test
  void routesAndRevalidatesDependenciesInOrder() {
    add(3, "architecture", "BLOCKED", correction("strategy"));
    assertThat(fresh("strategy")).isTrue();
    assertThat(readiness("strategy").ready()).isTrue();
    assertThat(readiness("strategy").reason()).contains("Atena", "Retorno 1 de 2", "Aceite");
    assertThat(readiness("economy").ready()).isFalse();
    assertThat(readiness("architecture").ready()).isFalse();
    add(4, "strategy", "IN_PROGRESS", null);
    assertThat(fresh("strategy")).isFalse();
    assertThat(readiness("architecture").ready()).isFalse();
    add(5, "strategy", "COMPLETED", approved("entrada corrigida"));
    assertThat(fresh("economy")).isTrue();
    assertThat(readiness("economy").ready()).isTrue();
    assertThat(readiness("architecture").reason()).contains("Plutus");
    add(6, "economy", "COMPLETED", approved("economia corrigida"));
    assertThat(readiness("architecture").ready()).isTrue();
    assertThat(fresh("architecture")).isTrue();
    add(7, "architecture", "COMPLETED", approved("arquitetura aceita"));
    for (var id : List.of("strategy", "economy", "architecture")) assertThat(fresh(id)).isFalse();
    assertThat(history.get(2).status()).isEqualTo("BLOCKED");
  }

  /** Troca de identificador e justificativa sem mudar o artefato não libera novo consumo. */
  @Test
  void rejectsCosmeticChangesAndRepeatedCommands() {
    add(3, "architecture", "BLOCKED", correction("strategy"));
    add(
        4,
        "strategy",
        "COMPLETED",
        approved("entrada antiga").replace("justificativa", "outra justificativa"));
    for (int attempt = 0; attempt < 4; attempt++) {
      assertThat(readiness("architecture").ready()).isFalse();
      assertThat(readiness("architecture").reason()).contains("não mudou a entrega funcional");
    }
    assertThat(history).hasSize(4);
  }

  /** Bloqueia o terceiro retorno mesmo se cada rodada mudou o artefato anterior. */
  @Test
  void capsReturnsIndependentlyOfRetryEpoch() {
    for (int round = 0; round < 3; round++) {
      add(3 + round * 3, "architecture", "BLOCKED", correction("strategy"));
      assertThat(readiness("strategy").ready()).isEqualTo(round < 2);
      if (round < 2) {
        add(4 + round * 3, "strategy", "COMPLETED", approved("correção " + round));
        add(5 + round * 3, "economy", "COMPLETED", approved("cálculo " + round));
      }
    }
    assertThat(readiness("strategy").reason())
        .contains("Limite de dois retornos", "retomar não reinicia");
  }

  /** Recusa destino próprio, inexistente ou humano sem inventar uma passagem alternativa. */
  @ParameterizedTest
  @ValueSource(strings = {"architecture", "unknown", "human"})
  void rejectsInvalidTargets(String target) {
    add(3, "architecture", "BLOCKED", correction(target));
    assertThat(fresh("strategy")).isFalse();
    assertThat(readiness("architecture").ready()).isFalse();
    assertThat(readiness("architecture").reason()).contains("predecessora válida");
  }

  /** Preserva uma falha na origem sem reenfileirar a própria tarefa bloqueada. */
  @Test
  void stopsAtBlockedOrigin() {
    add(3, "architecture", "BLOCKED", correction("strategy"));
    add(4, "strategy", "BLOCKED", "{\"decision\":\"ADJUST\"}");
    assertThat(fresh("strategy")).isFalse();
    assertThat(readiness("strategy").ready()).isFalse();
  }

  /** Produto distinto e ciclo encerrado não herdam o retorno; replay legado continua válido. */
  @Test
  void isolatesContextsAndPreservesHistoricalResults() {
    add(
        3,
        "architecture",
        "BLOCKED",
        "{\"decision\":\"ADJUST\",\"requiredChanges\":[\"ajuste histórico\"]}");
    assertThat(readiness("architecture").ready()).isTrue();
    add(4, "architecture", "BLOCKED", correction("strategy"));
    assertThat(
            gate.requiresFreshExecution(
                process, activity("strategy"), Product.builder().id(777L).build(), REF))
        .isFalse();
    cycle.setStatus("CLOSED");
    assertThat(fresh("strategy")).isFalse();
  }

  /** Reabre somente a economia quando o problema não afeta a estratégia aceita. */
  @Test
  void preservesUnaffectedPredecessors() {
    add(3, "architecture", "BLOCKED", correction("economy"));
    assertThat(fresh("strategy")).isFalse();
    assertThat(fresh("economy")).isTrue();
    add(4, "economy", "COMPLETED", approved("custos corrigidos"));
    assertThat(readiness("architecture").ready()).isTrue();
  }

  /** Declara um motivo e um aceite estruturados, sem dados comerciais. */
  private String correction(String target) {
    return "{\"decision\":\"ADJUST\",\"upstreamCorrection\":{\"contractVersion\":\"PROCESS_UPSTREAM_CORRECTION_V1\",\"targetActivityId\":\""
        + target
        + "\",\"reason\":\"Reconciliar a entrada existente\",\"acceptanceCriterion\":\"Preservar a especialidade já implementada\"}}";
  }

  /** Gera uma entrega funcional que permite comparar conteúdo além da justificativa. */
  private String approved(String content) {
    return "{\"decision\":\"APPROVE\",\"rationale\":\"justificativa\",\"contract\":{\"input\":\""
        + content
        + "\"}}";
  }

  /** Acrescenta tentativa sintética no mesmo processo e preserva as anteriores. */
  private void add(long id, String activity, String status, String result) {
    history.add(
        new AgentTaskFunctionalSnapshot(
            id,
            process.getId(),
            "fixture",
            activity,
            activity,
            status,
            cycle.getCreatedAt(),
            cycle.getCreatedAt(),
            result));
  }

  /** Constrói a identidade de uma atividade sem dependência de nomes de produtos. */
  private BusinessProcessActivityDefinition activity(String id) {
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId(id);
    return activity;
  }

  /** Consulta a necessidade de revalidação pelo contrato usado pelo motor real. */
  private boolean fresh(String id) {
    return gate.requiresFreshExecution(process, activity(id), product, REF);
  }

  /** Consulta a mensagem e a prontidão exibidas pela tela e pelo executor. */
  private AgentProductProcessActivityReadiness readiness(String id) {
    return gate.readiness(process, activity(id), product, REF);
  }
}
