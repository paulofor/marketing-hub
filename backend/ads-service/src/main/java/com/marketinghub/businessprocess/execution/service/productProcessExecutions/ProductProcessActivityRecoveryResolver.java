package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsabilidade: ligar cards bloqueados às ações de recuperação declaradas no mesmo processo.
 */
@Slf4j
public final class ProductProcessActivityRecoveryResolver {

  /** Restringe o uso à projeção pura, sem instanciação nem acesso a persistência. */
  private ProductProcessActivityRecoveryResolver() {}

  /** Usa metadados canônicos e disponibilidade já calculada para evitar regras locais na tela. */
  public static List<ProductProcessActivityExecutionGroupResponse> resolve(
      List<ProductProcessActivityExecutionGroupResponse> groups,
      Map<String, BusinessProcessActivityDefinition> definitions,
      ObjectMapper json) {
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
                candidate.activityOwnerName(),
                control.actionLabel(),
                control.actionAvailable(),
                control.availabilityReason());
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
                        && "BLOCKED".equals(group.operationalState())
                    ? group.withRecoveryAction(recoveries.get(group.activityId()))
                    : group)
        .toList();
  }
}
