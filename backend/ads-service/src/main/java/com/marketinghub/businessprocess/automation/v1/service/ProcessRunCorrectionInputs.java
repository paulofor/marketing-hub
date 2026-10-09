package com.marketinghub.businessprocess.automation.v1.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskFunctionalSnapshot;
import com.marketinghub.businessprocess.automation.v1.ProcessRun;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionGroupResponse;
import com.marketinghub.businessprocess.execution.service.recentExecutions.BusinessProcessActivityExecutionResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsabilidade: identificar pareceres novos que motivam uma correção já liberada pelo domínio.
 */
@Slf4j
final class ProcessRunCorrectionInputs {
  /** Impede instanciação de uma projeção pura dos contratos persistidos. */
  private ProcessRunCorrectionInputs() {}

  /**
   * Considera somente ajustes posteriores à produção e pertencentes à mesma referência e definição.
   * A projeção funcional deve corresponder ao resumo, sem depender de comentários de auditoria. A
   * própria falha e reavaliações antigas nunca liberam repetição.
   */
  static List<String> resolve(
      ProcessRun run,
      ProductProcessActivityExecutionGroupResponse activity,
      List<ProductProcessActivityExecutionGroupResponse> ordered,
      ProcessExecutionGraph graph,
      ObjectMapper json,
      List<AgentTaskFunctionalSnapshot> functionalTasks) {
    if (activity.objectiveAchieved() || !activity.executionRequestAvailable()) return List.of();
    long produced =
        activity.tasks().stream()
            .filter(t -> sameScope(run, t) && "COMPLETED".equals(t.status()))
            .mapToLong(BusinessProcessActivityExecutionResponse::taskId)
            .max()
            .orElse(0L);
    List<String> inputs = new ArrayList<>();
    for (var reviewer : ordered) {
      if (reviewer.activityId().equals(activity.activityId())
          || !reviewer.selectedVersionActivity()
          || reviewer.objectiveAchieved()
          || !graph.canSupplyCorrection(activity.activityId(), reviewer.activityId())) continue;
      var latest =
          reviewer.tasks().stream()
              .filter(t -> sameScope(run, t))
              .max(Comparator.comparing(BusinessProcessActivityExecutionResponse::taskId));
      if (latest.isEmpty()) continue;
      var task = latest.get();
      if (task.taskId() <= produced || !"BLOCKED".equals(task.status())) continue;
      var persisted =
          functionalTasks.stream()
              .filter(
                  value ->
                      Objects.equals(task.taskId(), value.id())
                          && Objects.equals(
                              run.getProcessDefinitionId(), value.processDefinitionId())
                          && Objects.equals(reviewer.activityId(), value.processActivityId())
                          && Objects.equals(task.status(), value.status()))
              .findFirst();
      if (persisted.isEmpty() || persisted.get().resultJson() == null) continue;
      try {
        var result = json.readTree(persisted.get().resultJson());
        if (result == null
            || !"ADJUST".equals(result.path("decision").asText())
            || !result.path("requiredChanges").isArray()
            || result.path("requiredChanges").isEmpty()) continue;
        inputs.add(reviewer.activityId() + ":" + task.taskId() + ":" + result);
      } catch (Exception ex) {
        log.warn(
            "Parecer inválido na entrada de correção. runId={} activityId={} taskId={}",
            run.getId(),
            activity.activityId(),
            task.taskId(),
            ex);
      }
    }
    return List.copyOf(inputs);
  }

  /** Recusa parecer de outro produto, ciclo ou versão mesmo em uma projeção histórica ampliada. */
  private static boolean sameScope(ProcessRun run, BusinessProcessActivityExecutionResponse task) {
    return task.taskId() != null
        && Objects.equals(run.getProcessDefinitionId(), task.processDefinitionId())
        && Objects.equals(run.getSourceReference(), task.sourceReference());
  }
}
