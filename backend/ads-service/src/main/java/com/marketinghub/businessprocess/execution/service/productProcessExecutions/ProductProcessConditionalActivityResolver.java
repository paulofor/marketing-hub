package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsabilidade: distinguir tentativas condicionais históricas de recuperações ainda exigidas.
 */
@Slf4j
public final class ProductProcessConditionalActivityResolver {
  private static final Set<String> RESOLVED_WITHOUT_OBJECTIVE =
      Set.of("HISTORICAL", "NOT_APPLICABLE", "RECORDED");
  private static final String RECORDED_REASON =
      "Tentativa condicional encerrada preservada no histórico; os objetivos que acionavam a recuperação já foram comprovados.";

  /** Impede que recuperação cancelada ou bloqueada permaneça pendente após todos os aceites. */
  public static List<ProductProcessActivityExecutionGroupResponse> resolve(
      List<ProductProcessActivityExecutionGroupResponse> groups,
      Map<String, BusinessProcessActivityDefinition> definitions,
      ObjectMapper json) {
    Map<String, ProductProcessActivityExecutionGroupResponse> byActivityId =
        groups.stream()
            .collect(
                Collectors.toMap(
                    ProductProcessActivityExecutionGroupResponse::activityId,
                    Function.identity(),
                    (first, ignored) -> first));
    return groups.stream()
        .map(group -> recordInactiveRecovery(group, byActivityId, definitions, json))
        .toList();
  }

  /** Registra recuperação inativa somente quando todos os destinos deixaram de exigir reparo. */
  private static ProductProcessActivityExecutionGroupResponse recordInactiveRecovery(
      ProductProcessActivityExecutionGroupResponse group,
      Map<String, ProductProcessActivityExecutionGroupResponse> byActivityId,
      Map<String, BusinessProcessActivityDefinition> definitions,
      ObjectMapper json) {
    if (!group.selectedVersionActivity()
        || !Set.of("CANCELLED", "BLOCKED").contains(group.operationalState())
        || group.executionRequestAvailable()) return group;
    var definition = definitions.get(group.activityId());
    if (definition == null
        || definition.getDefinitionJson() == null
        || definition.getDefinitionJson().isBlank()) return group;
    try {
      var metadata = json.readTree(definition.getDefinitionJson());
      if (!"ON_FUNCTIONAL_REJECTION".equals(metadata.path("activationMode").asText())) {
        return group;
      }
      var targets = metadata.path("remediatesActivities");
      if (!targets.isArray() || targets.isEmpty()) return group;
      for (var target : targets) {
        if (!target.isTextual()) return group;
        var targetActivity = byActivityId.get(target.asText());
        if (targetActivity == null || !resolved(targetActivity)) return group;
      }
      return recorded(group);
    } catch (Exception ex) {
      log.error(
          "Falha ao classificar atividade condicional inativa. activityDefinitionId={} activityId={}",
          definition.getId(),
          group.activityId(),
          ex);
      return group;
    }
  }

  /** Reconhece objetivo comprovado ou dispensa explícita sem transformar omissão em aprovação. */
  private static boolean resolved(ProductProcessActivityExecutionGroupResponse activity) {
    return !activity.selectedVersionActivity()
        || activity.objectiveAchieved()
        || RESOLVED_WITHOUT_OBJECTIVE.contains(activity.operationalState());
  }

  /** Preserva tarefas e instância originais, alterando apenas a projeção operacional derivada. */
  private static ProductProcessActivityExecutionGroupResponse recorded(
      ProductProcessActivityExecutionGroupResponse group) {
    var control = group.executionControl();
    if (control != null) {
      control =
          new ProductProcessActivityExecutionControlResponse(
              control.executorType(),
              control.interactionType(),
              control.actionLabel(),
              control.description(),
              false,
              RECORDED_REASON,
              control.confirmationRequired(),
              control.confirmationTitle(),
              control.confirmationMessage(),
              control.confirmationToken(),
              control.workspaceCode(),
              control.workspaceReferenceId(),
              control.targetProcessDefinitionId(),
              control.requirements(),
              control.decisionMode(),
              control.auditEvidenceReference(),
              control.navigationUrl());
    }
    return new ProductProcessActivityExecutionGroupResponse(
        group.activityDefinitionId(),
        group.activityId(),
        group.activityName(),
        group.activityObjective(),
        group.activityOwnerName(),
        group.sequenceNumber(),
        true,
        "RECORDED",
        RECORDED_REASON,
        false,
        "RECORDED_CONDITIONAL_RECOVERY",
        group.activityInstanceId(),
        group.occurrenceNumber(),
        group.taskCount(),
        group.tasks(),
        false,
        RECORDED_REASON,
        control,
        group.recoveryAction());
  }

  /** Restringe o uso à projeção pura, sem acesso a persistência nem execução de tarefas. */
  private ProductProcessConditionalActivityResolver() {}
}
