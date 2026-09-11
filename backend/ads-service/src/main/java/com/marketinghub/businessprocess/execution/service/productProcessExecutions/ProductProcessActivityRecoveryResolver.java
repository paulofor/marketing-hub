package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsabilidade: ligar cards bloqueados às ações de recuperação declaradas no mesmo processo.
 */
@Slf4j
public final class ProductProcessActivityRecoveryResolver {

  /** Restringe o uso à projeção pura, sem instanciação nem acesso a persistência. */
  private ProductProcessActivityRecoveryResolver() {}

  /** Expõe a atividade de correção ainda necessária na mesma versão e referência operacional. */
  public static List<ProductProcessActivityExecutionGroupResponse> resolve(
      List<ProductProcessActivityExecutionGroupResponse> groups,
      Map<String, BusinessProcessActivityDefinition> definitions,
      ObjectMapper json,
      Long processDefinitionId,
      String sourceReference) {
    Map<String, ProductProcessActivityRecoveryResponse> recoveries = new LinkedHashMap<>();
    for (var candidate : groups) {
      var definition = definitions.get(candidate.activityId());
      var control = candidate.executionControl();
      if (definition == null
          || definition.getDefinitionJson() == null
          || !candidate.selectedVersionActivity()
          || candidate.objectiveAchieved()
          || control == null
          || !"AGENT".equals(control.executorType())
          || !"COMMAND".equals(control.interactionType())
          || control.actionLabel() == null) continue;
      try {
        var targets = json.readTree(definition.getDefinitionJson()).path("remediatesActivities");
        if (!targets.isArray()) continue;
        var recovery =
            new ProductProcessActivityRecoveryResponse(
                candidate.activityId(),
                candidate.activityName(),
                candidate.sequenceNumber(),
                candidate.activityOwnerName(),
                actionLabel(candidate),
                control.actionAvailable(),
                control.availabilityReason(),
                candidate.operationalState(),
                candidate.objectiveAchieved(),
                candidate.tasks().stream()
                    .filter(task -> Objects.equals(processDefinitionId, task.processDefinitionId()))
                    .filter(task -> Objects.equals(sourceReference, task.sourceReference()))
                    .max(Comparator.comparing(task -> task.taskId()))
                    .map(ProductProcessRecoveryTaskResponse::from)
                    .orElse(null));
        for (var target : targets) {
          if (target.isTextual() && !candidate.activityId().equals(target.asText())) {
            recoveries.putIfAbsent(target.asText(), recovery);
          }
        }
      } catch (Exception ex) {
        log.error(
            "Falha ao ler destino de recuperação BPM. activityDefinitionId={} activityId={}",
            definition.getId(),
            candidate.activityId(),
            ex);
      }
    }
    return groups.stream()
        .map(
            group ->
                group.selectedVersionActivity()
                        && !group.objectiveAchieved()
                        && !group.executionRequestAvailable()
                        && ("BLOCKED".equals(group.operationalState())
                            || (recoveries.containsKey(group.activityId())
                                && recoveries.get(group.activityId()).latestTask() != null))
                    ? group.withRecoveryAction(recoveries.get(group.activityId()))
                    : group)
        .toList();
  }

  /** Mantém uma única ação distinguindo criação, espera e conclusão sem sugerir duplicação. */
  private static String actionLabel(ProductProcessActivityExecutionGroupResponse candidate) {
    return switch (candidate.operationalState()) {
      case "PENDING" -> "Tarefa na fila";
      case "IN_PROGRESS" -> "Tarefa em execução";
      case "COMPLETED" -> "Correção concluída";
      default -> candidate.executionControl().actionLabel();
    };
  }
}
