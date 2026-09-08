package com.marketinghub.businessprocesschain.learningcycle.v1.service.getCycles;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Responsabilidade: expor o BPM versionado, experimentos elegíveis e destinos reais de retorno. */
public record LearningCycleCatalog(
    Long processDefinitionId,
    int version,
    JsonNode diagram,
    List<Target> returnTargets,
    List<ExperimentOption> experiments,
    LearningCycleEntry entry) {
  /** Identifica uma atividade real que pode receber a correção causal. */
  public record Target(
      Long processDefinitionId,
      String processName,
      String activityId,
      String activityName,
      String owner,
      String processCode) {}

  /** Identifica um experimento do produto e sua disponibilidade explícita para adoção. */
  public record ExperimentOption(
      Long id, String name, String status, boolean available, boolean baseline, String reason) {}
}
