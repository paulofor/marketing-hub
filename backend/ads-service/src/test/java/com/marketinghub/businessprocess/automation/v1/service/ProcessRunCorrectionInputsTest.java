package com.marketinghub.businessprocess.automation.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Responsabilidade: proteger a identidade causal de uma correção contra repetição e mistura de
 * provas.
 */
class ProcessRunCorrectionInputsTest {
  private final ObjectMapper json = new ObjectMapper();
  private final ProcessRun run = run();

  /** Parecer posterior muda a entrada; repetir a consulta mantém a mesma identidade auditável. */
  @Test
  void recognizesNewAdjustmentAndKeepsRepeatedInputStable() throws Exception {
    var producer = group("producer", task(100, "COMPLETED", null));
    var reviewer = group("reviewer", task(101, "BLOCKED", adjustment()));
    var before = resolve(producer, group("reviewer"));
    var after = resolve(producer, reviewer);
    assertThat(before).isEmpty();
    assertThat(after).hasSize(1).isEqualTo(resolve(producer, reviewer));
    assertThat(after.getFirst()).contains("reviewer:101:", "Ampliar a prova");
  }

  /** Falha da correção não cria uma entrada nova nem apaga o parecer que motivou a tentativa. */
  @Test
  void ownFailureDoesNotCreateAnotherCorrectionInput() throws Exception {
    var original = task(100, "COMPLETED", null);
    var reviewer = group("reviewer", task(101, "BLOCKED", adjustment()));
    assertThat(resolve(group("producer", original, task(102, "BLOCKED", adjustment())), reviewer))
        .isEqualTo(resolve(group("producer", original), reviewer));
  }

  /** Revisão antiga, técnica ou sem instrução válida não libera consumo automático. */
  @Test
  void rejectsOldTechnicalMalformedAndIncompleteReviews() throws Exception {
    var producer = group("producer", task(100, "COMPLETED", null));
    assertThat(resolve(producer, group("reviewer", task(99, "BLOCKED", adjustment())))).isEmpty();
    for (String invalid :
        List.of(
            "{",
            "null",
            "{\"decision\":\"BLOCKED\"}",
            "{\"decision\":\"ADJUST\",\"requiredChanges\":[]}")) {
      assertThat(resolve(producer, group("reviewer", task(101, "BLOCKED", invalid)))).isEmpty();
    }
  }

  /** Uma aprovação posterior prevalece sobre rejeição histórica da mesma atividade. */
  @Test
  void latestReviewSupersedesHistoricRejection() throws Exception {
    assertThat(
            resolve(
                group("producer", task(100, "COMPLETED", null)),
                group(
                    "reviewer", task(101, "BLOCKED", adjustment()), task(102, "COMPLETED", "{}"))))
        .isEmpty();
  }

  /** Nenhuma referência, definição ou atividade independente pode autorizar retrabalho cruzado. */
  @Test
  void isolatesSourceDefinitionAndDependency() throws Exception {
    var producer = group("producer", task(100, "COMPLETED", null));
    var foreign = task(101, "BLOCKED", adjustment());
    when(foreign.sourceReference()).thenReturn("product:999999@agent-validation-v1");
    assertThat(resolve(producer, group("reviewer", foreign))).isEmpty();
    when(foreign.sourceReference()).thenReturn(run.getSourceReference());
    when(foreign.processDefinitionId()).thenReturn(99999L);
    assertThat(resolve(producer, group("reviewer", foreign))).isEmpty();
    assertThat(resolve(producer, group("unrelated", task(101, "BLOCKED", adjustment())))).isEmpty();
  }

  /** Nova produção encerra a validade causal do parecer anterior até uma nova avaliação. */
  @Test
  void completedCorrectionRequiresNewerReviewBeforeAnotherRework() throws Exception {
    assertThat(
            resolve(
                group("producer", task(100, "COMPLETED", null), task(102, "COMPLETED", null)),
                group("reviewer", task(101, "BLOCKED", adjustment()))))
        .isEmpty();
  }

  /** A prontidão do domínio continua obrigatória; a chave nunca libera uma atividade por si só. */
  @Test
  void doesNotReopenCompletedOrUnavailableActivity() throws Exception {
    var producer = group("producer", task(100, "COMPLETED", null));
    var reviewer = group("reviewer", task(101, "BLOCKED", adjustment()));
    when(producer.objectiveAchieved()).thenReturn(true);
    assertThat(resolve(producer, reviewer)).isEmpty();
    when(producer.objectiveAchieved()).thenReturn(false);
    when(producer.executionRequestAvailable()).thenReturn(false);
    assertThat(resolve(producer, reviewer)).isEmpty();
  }

  /** Parecer persistido continua utilizável quando o resumo omite comentários e auditoria. */
  @ParameterizedTest
  @ValueSource(longs = {94110L, 95110L})
  void readsPersistedAdjustmentWithoutSummaryCommentsAndRejectsMismatchedSnapshot(long productId)
      throws Exception {
    run.setProductId(productId);
    run.setProcessDefinitionId(productId + 54);
    run.setSourceReference("product:" + productId + "@agent-validation-v1");
    var producer = group("producer", task(100, "COMPLETED", null));
    var reviewer = group("reviewer", task(101, "BLOCKED", null));
    var persisted =
        functionalTask(101L, run.getProcessDefinitionId(), "reviewer", "BLOCKED", adjustment());
    assertThat(resolve(producer, reviewer, List.of(persisted))).hasSize(1);
    assertThat(resolve(producer, reviewer, List.of())).isEmpty();
    for (var mismatch :
        List.of(
            functionalTask(102L, run.getProcessDefinitionId(), "reviewer", "BLOCKED", adjustment()),
            functionalTask(101L, 99999L, "reviewer", "BLOCKED", adjustment()),
            functionalTask(
                101L, run.getProcessDefinitionId(), "unrelated", "BLOCKED", adjustment()),
            functionalTask(
                101L, run.getProcessDefinitionId(), "reviewer", "COMPLETED", adjustment()))) {
      assertThat(resolve(producer, reviewer, List.of(mismatch))).isEmpty();
    }
  }

  /** Resolve o contrato contra um grafo mínimo com revisão dependente e atividade independente. */
  private List<String> resolve(
      ProductProcessActivityExecutionGroupResponse producer,
      ProductProcessActivityExecutionGroupResponse reviewer)
      throws Exception {
    var functionalTasks =
        reviewer.tasks().stream()
            .map(
                task ->
                    functionalTask(
                        task.taskId(),
                        task.processDefinitionId(),
                        reviewer.activityId(),
                        task.status(),
                        task.comments()))
            .toList();
    return resolve(producer, reviewer, functionalTasks);
  }

  /** Resolve o grafo com resultados funcionais persistidos separados do histórico resumido. */
  private List<String> resolve(
      ProductProcessActivityExecutionGroupResponse producer,
      ProductProcessActivityExecutionGroupResponse reviewer,
      List<AgentTaskFunctionalSnapshot> functionalTasks)
      throws Exception {
    var graph =
        new ProcessExecutionGraph(
            json.readTree(
                """
        {"nodes":[{"id":"producer","type":"TASK"},{"id":"reviewer","type":"TASK"},
          {"id":"unrelated","type":"TASK"}],"flows":[{"from":"producer","to":"reviewer"}]}
        """));
    return ProcessRunCorrectionInputs.resolve(
        run, producer, List.of(producer, reviewer), graph, json, functionalTasks);
  }

  /** Monta a projeção funcional sem prompt, evidência bruta ou integração externa. */
  private AgentTaskFunctionalSnapshot functionalTask(
      long id, Long definition, String activity, String status, String result) {
    return new AgentTaskFunctionalSnapshot(
        id, definition, "QA_PROCESS", activity, "qa-reviewer", status, null, null, result);
  }

  /** Cria somente contratos de acompanhamento, sem auditoria ou integrações externas. */
  private ProductProcessActivityExecutionGroupResponse group(
      String id, BusinessProcessActivityExecutionResponse... tasks) {
    var group = mock(ProductProcessActivityExecutionGroupResponse.class);
    when(group.activityId()).thenReturn(id);
    when(group.selectedVersionActivity()).thenReturn(true);
    when(group.executionRequestAvailable()).thenReturn(true);
    when(group.tasks()).thenReturn(List.of(tasks));
    return group;
  }

  /** Prepara uma tarefa sintética dentro da identidade da execução. */
  private BusinessProcessActivityExecutionResponse task(long id, String status, String result) {
    var task = mock(BusinessProcessActivityExecutionResponse.class);
    when(task.taskId()).thenReturn(id);
    when(task.processDefinitionId()).thenReturn(run.getProcessDefinitionId());
    when(task.sourceReference()).thenReturn(run.getSourceReference());
    when(task.status()).thenReturn(status);
    when(task.comments()).thenReturn(result);
    return task;
  }

  /** Produz instrução funcional que não se confunde com erro de integração. */
  private String adjustment() {
    return "{\"decision\":\"ADJUST\",\"requiredChanges\":[\"Ampliar a prova\"]}";
  }

  /** Mantém os identificadores de QA separados dos registros comerciais. */
  private static ProcessRun run() {
    var run = new ProcessRun();
    run.setId(94106L);
    run.setProcessDefinitionId(94164L);
    run.setSourceReference("product:94110@agent-validation-v1");
    return run;
  }
}
