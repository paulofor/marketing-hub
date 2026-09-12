package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger a ordem do BPM e impedir avanço sem evidência funcional. */
class ProcessExecutionGraphTest {
  /**
   * Preserva compatibilidade com as doze topologias publicadas consultadas no MCP em 12/09/2026.
   */
  @Test
  void acceptsPublishedProductTopologiesAndTheirRework() throws Exception {
    var data =
        new ObjectMapper()
            .readTree(
                getClass()
                    .getResourceAsStream(
                        "/process-automation/published-topologies-2026-09-12.json"));
    assertThat(data.size()).isEqualTo(12);
    for (var row : data) {
      var graph = new ProcessExecutionGraph(row.path("diagram"));
      var tasks = new java.util.ArrayList<ProductProcessActivityExecutionGroupResponse>();
      for (var node : row.path("diagram").path("nodes"))
        if ("TASK".equals(node.path("type").asText()))
          tasks.add(activity(node.path("id").asText(), false));
      assertThat(graph.ordered(tasks)).as(row.path("processCode").asText()).hasSize(tasks.size());
    }
  }

  /** A ordem das definições no JSON não pode substituir a ordem causal do grafo. */
  @Test
  void ordersByGraphAndRequiresFunctionalPredecessors() throws Exception {
    var graph =
        new ProcessExecutionGraph(
            new ObjectMapper()
                .readTree(
                    """
        {"nodes":[{"id":"second","type":"TASK"},{"id":"first","type":"TASK"}],
         "flows":[{"from":"first","to":"second"}]}
        """));
    var first = activity("first", false);
    var second = activity("second", false);
    assertThat(graph.ordered(List.of(second, first))).containsExactly(first, second);
    assertThat(graph.predecessorsSatisfied("first", List.of(first, second))).isTrue();
    assertThat(graph.predecessorsSatisfied("second", List.of(first, second))).isFalse();
    when(first.operationalState()).thenReturn("COMPLETED");
    assertThat(graph.predecessorsSatisfied("second", List.of(first, second))).isFalse();
    when(first.objectiveAchieved()).thenReturn(true);
    assertThat(graph.predecessorsSatisfied("second", List.of(first, second))).isTrue();
  }

  /** Retornos são explícitos e um ciclo não declarado impede execução do processo. */
  @Test
  void rejectsCyclesButAcceptsExplicitRework() throws Exception {
    var json = new ObjectMapper();
    String diagram =
        """
        {"nodes":[{"id":"a","type":"TASK"},{"id":"b","type":"TASK"}],
         "flows":[{"from":"a","to":"b"},{"from":"b","to":"a","kind":"%s"}]}
        """;
    assertThatThrownBy(
            () -> new ProcessExecutionGraph(json.readTree(diagram.formatted("SEQUENCE"))))
        .isInstanceOf(IllegalStateException.class);
    assertThatCode(() -> new ProcessExecutionGraph(json.readTree(diagram.formatted("REWORK"))))
        .doesNotThrowAnyException();
  }

  /** A dispensa precisa ser declarada no contrato oficial e não altera o objetivo da atividade. */
  @Test
  void permitsExplicitOmission() throws Exception {
    var graph =
        new ProcessExecutionGraph(
            new ObjectMapper()
                .readTree(
                    """
        {"nodes":[{"id":"a","type":"TASK"},{"id":"b","type":"TASK"}],"flows":[{"from":"a","to":"b"}]}
        """));
    var first = activity("a", false);
    when(first.operationalState()).thenReturn("NOT_APPLICABLE");
    assertThat(graph.predecessorsSatisfied("b", List.of(first, activity("b", false)))).isTrue();
    assertThat(first.objectiveAchieved()).isFalse();
  }

  /** Nenhuma atividade ausente ou de outra versão pode desaparecer silenciosamente do controle. */
  @Test
  void rejectsMissingOrUnmappedActivityContracts() throws Exception {
    var graph =
        new ProcessExecutionGraph(
            new ObjectMapper()
                .readTree(
                    """
        {"nodes":[{"id":"a","type":"TASK"},{"id":"b","type":"TASK"}],
         "flows":[{"from":"a","to":"b"}]}
        """));
    var first = activity("a", true);
    assertThatThrownBy(() -> graph.ordered(List.of(first)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("b");
    var legacy = activity("b", true);
    when(legacy.activityDefinitionId()).thenReturn(null);
    when(legacy.selectedVersionActivity()).thenReturn(false);
    assertThatThrownBy(() -> graph.ordered(List.of(first, legacy)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("b");
    var second = activity("b", false);
    assertThatThrownBy(() -> graph.ordered(List.of(first, second, activity("unknown", true))))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("unknown");
    assertThat(graph.ordered(List.of(first, second))).containsExactly(first, second);
    when(second.selectedVersionActivity()).thenReturn(false);
    assertThat(graph.ordered(List.of(first, second))).containsExactly(first, second);
  }

  /** Cria projeção mínima com identidade e estado oficial da atividade. */
  private ProductProcessActivityExecutionGroupResponse activity(String id, boolean achieved) {
    var activity = mock(ProductProcessActivityExecutionGroupResponse.class);
    when(activity.activityId()).thenReturn(id);
    when(activity.activityDefinitionId()).thenReturn(1L);
    when(activity.selectedVersionActivity()).thenReturn(true);
    when(activity.objectiveAchieved()).thenReturn(achieved);
    when(activity.operationalState()).thenReturn("NOT_STARTED");
    return activity;
  }
}
