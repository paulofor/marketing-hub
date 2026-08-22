package com.marketinghub.growthoperatorworker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Responsabilidade: proteger o consumo auditável do processo de otimização por Hermes. */
class GrowthOperatorBpmTaskConsumerTest {
  private final ObjectMapper json = new ObjectMapper();

  /** Conclui a atividade comprovada e envia tokens reais para cálculo centralizado do custo. */
  @Test
  void shouldCompleteEligibleExperimentActivityWithModelUsage() throws Exception {
    GrowthOperatorBackendClient backend = mock(GrowthOperatorBackendClient.class);
    GrowthOperatorBpmRunner runner = mock(GrowthOperatorBpmRunner.class);
    WorkerProperties properties = properties();
    Map<String, Object> task = task(88L);
    when(backend.claimBpmTask("operacao-otimizacao-experimento", "task-1")).thenReturn(task);
    when(runner.run(task))
        .thenReturn(
            new GrowthOperatorBpmRunner.BpmExecution(
                result("COMPLETED"),
                new GrowthOperatorBpmRunner.TokenUsage(120L, 20L, 40L),
                List.of()));

    new GrowthOperatorBpmTaskConsumer(backend, runner, properties, json).processOne();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
    verify(backend).completeBpmTask(eq(88L), payload.capture());
    assertThat(payload.getValue().get("resultJson").toString()).contains("COMPLETED");
    assertThat(payload.getValue().get("evidenceJson").toString())
        .contains("READ_ONLY", "externalSideEffects", "experiment:88");
    assertThat(payload.getValue().get("modelUsages").toString())
        .contains("gpt-5.6-sol", "inputTokens=120", "cachedInputTokens=20", "outputTokens=40");
  }

  /**
   * Mantém a etapa bloqueada quando a amostra ou a instrumentação ainda não comprovam o objetivo.
   */
  @Test
  void shouldBlockActivityWithoutAdvancingProcess() throws Exception {
    GrowthOperatorBackendClient backend = mock(GrowthOperatorBackendClient.class);
    GrowthOperatorBpmRunner runner = mock(GrowthOperatorBpmRunner.class);
    Map<String, Object> task = task(89L);
    when(backend.claimBpmTask("operacao-otimizacao-experimento", "task-1")).thenReturn(task);
    when(runner.run(task))
        .thenReturn(
            new GrowthOperatorBpmRunner.BpmExecution(
                result("BLOCKED"), GrowthOperatorBpmRunner.TokenUsage.empty(), List.of()));

    new GrowthOperatorBpmTaskConsumer(backend, runner, properties(), json).processOne();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
    verify(backend).failBpmTask(eq(89L), payload.capture());
    assertThat(payload.getValue().get("error").toString()).contains("Hermes bloqueou o avanço");
    assertThat(payload.getValue()).doesNotContainKey("modelUsages");
  }

  /** Não busca atividades posteriores quando a primeira já forneceu trabalho. */
  @Test
  void shouldRespectPublishedActivityOrder() throws Exception {
    GrowthOperatorBackendClient backend = mock(GrowthOperatorBackendClient.class);
    GrowthOperatorBpmRunner runner = mock(GrowthOperatorBpmRunner.class);
    Map<String, Object> task = task(90L);
    when(backend.claimBpmTask("operacao-otimizacao-experimento", "task-1")).thenReturn(task);
    when(runner.run(task))
        .thenReturn(
            new GrowthOperatorBpmRunner.BpmExecution(
                result("COMPLETED"), GrowthOperatorBpmRunner.TokenUsage.empty(), List.of()));

    new GrowthOperatorBpmTaskConsumer(backend, runner, properties(), json).processOne();

    verify(backend).claimBpmTask("operacao-otimizacao-experimento", "task-1");
    verify(backend).completeBpmTask(eq(90L), any());
  }

  /** Prioriza e conclui o contrato da jornada do PDE com referência do plano preservada. */
  @Test
  void shouldCompletePdeCommunicationContract() throws Exception {
    GrowthOperatorBackendClient backend = mock(GrowthOperatorBackendClient.class);
    GrowthOperatorBpmRunner runner = mock(GrowthOperatorBpmRunner.class);
    Map<String, Object> task =
        Map.of(
            "taskId", 91L,
            "activityId", "contract",
            "sourceReference", "commercial-plan:4@v2",
            "processCode", "pde-communication-sales-journey");
    when(backend.claimBpmTask("pde-communication-sales-journey", "contract")).thenReturn(task);
    when(runner.run(task))
        .thenReturn(
            new GrowthOperatorBpmRunner.BpmExecution(
                result("COMPLETED"), GrowthOperatorBpmRunner.TokenUsage.empty(), List.of()));

    new GrowthOperatorBpmTaskConsumer(backend, runner, properties(), json).processOne();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
    verify(backend).completeBpmTask(eq(91L), payload.capture());
    assertThat(payload.getValue().get("evidenceJson").toString())
        .contains("commercial-plan:4@v2", "externalSideEffects");
  }

  /** Cria uma resposta mínima válida para exercitar os callbacks. */
  private com.fasterxml.jackson.databind.JsonNode result(String status) throws Exception {
    return json.readTree(
        """
        {
          "executionStatus":"%s",
          "activityOutcome":"A integridade foi avaliada com evidência persistida.",
          "observedFacts":["Campanha consultada"],
          "inferences":[],
          "contradictoryEvidence":[],
          "evidenceGaps":[],
          "alternatives":[
            {"name":"A","benefit":"B","risk":"R","effort":"E","fit":"F"},
            {"name":"B","benefit":"B","risk":"R","effort":"E","fit":"F"},
            {"name":"C","benefit":"B","risk":"R","effort":"E","fit":"F"}
          ],
          "selectedAlternative":"A",
          "expectedMetric":"Eventos íntegros",
          "continueCriteria":"Integridade comprovada",
          "adjustCriteria":"Divergência encontrada",
          "stopCriteria":"Risco comercial",
          "recommendedAction":"Corrigir a instrumentação antes de avançar."
        }
        """
            .formatted(status));
  }

  /** Cria o contrato congelado de um experimento. */
  private Map<String, Object> task(Long id) {
    return Map.of(
        "taskId", id,
        "activityId", "task-1",
        "sourceReference", "experiment:88",
        "processCode", "operacao-otimizacao-experimento");
  }

  /** Configura modelo e URLs usados na evidência. */
  private WorkerProperties properties() {
    WorkerProperties properties = new WorkerProperties();
    properties.setModel("gpt-5.6-sol");
    properties.setBackendUrl("http://backend:8000");
    properties.setMarketingHubUrl("http://backend:8000");
    return properties;
  }
}
