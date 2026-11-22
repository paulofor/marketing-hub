package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunQueueBlocker;
import com.marketinghub.businessprocess.automation.v1.service.status.ProcessRunResponse;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir vazamento de erros técnicos nas projeções públicas da continuidade. */
class ContextFailureBusinessProjectionTest {
  private final ObjectMapper json = new ObjectMapper();

  /**
   * Cobre registros legados e outro contexto, mantendo contadores, custo desconhecido e auditoria.
   */
  @Test
  void projectsWholePageWithoutMutatingTaskDiagnosticOrAcceptance() throws Exception {
    for (long id : new long[] {922L, 12345L}) {
      String raw = "com.internal.Exception: input_too_large {raw:true}";
      var history = json.createObjectNode();
      history
          .put("productId", id)
          .put("currentActivityStateReason", raw)
          .put("completedActivityCount", 0)
          .put("remainingActivityCount", 11);
      var group = history.putArray("activities").addObject();
      group
          .put("stateReason", raw)
          .put("executionRequestReason", raw)
          .put("objectiveAchieved", false)
          .put("operationalState", "BLOCKED");
      group
          .putArray("tasks")
          .addObject()
          .put("taskId", id)
          .put("status", "BLOCKED")
          .put("executionError", raw)
          .put("costEstimationStatus", "NOT_REPORTED");
      var projected =
          json.treeToValue(history, ProductProcessActivityExecutionHistoryResponse.class);
      assertThat(projected.currentActivityStateReason())
          .contains("interrompida")
          .doesNotContain(raw);
      assertThat(projected.completedActivityCount()).isZero();
      assertThat(projected.remainingActivityCount()).isEqualTo(11);
      var activity = projected.activities().getFirst();
      assertThat(activity.stateReason()).doesNotContain(raw);
      assertThat(activity.executionRequestReason()).doesNotContain(raw);
      assertThat(activity.objectiveAchieved()).isFalse();
      assertThat(activity.operationalState()).isEqualTo("BLOCKED");
      assertThat(activity.tasks().getFirst().executionError()).isEqualTo(raw);
      assertThat(activity.tasks().getFirst().estimatedCostUsd()).isNull();
      assertThat(history.path("currentActivityStateReason").asText()).isEqualTo(raw);
      var run = json.createObjectNode().put("id", id).put("reason", raw).put("status", "BLOCKED");
      assertThat(json.treeToValue(run, ProcessRunResponse.class).reason()).doesNotContain(raw);
      run.remove("id");
      assertThat(json.treeToValue(run, ProcessRunQueueBlocker.class).reason()).doesNotContain(raw);
    }
  }
}
