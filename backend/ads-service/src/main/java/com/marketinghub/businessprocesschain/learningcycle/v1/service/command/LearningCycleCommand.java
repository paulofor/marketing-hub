package com.marketinghub.businessprocesschain.learningcycle.v1.service.command;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.util.UUID;

/** Responsabilidade: transportar uma decisão explícita com revisão, autoria e evidência. */
public record LearningCycleCommand(
    @NotNull UUID requestKey,
    @Min(0) long expectedRevision,
    @NotNull Action action,
    @NotBlank @Size(max = 160) String operatorName,
    @NotBlank @Size(max = 4000) String summary,
    @NotBlank @Size(max = 1200) String evidenceReference,
    @NotNull JsonNode evidence) {
  /** Define os movimentos permitidos pelo contrato do ciclo, sem comandos de gasto externo. */
  public enum Action {
    COMPLETE,
    REWORK,
    MEASURE,
    ADJUST,
    CONTINUE,
    FIX_MEASUREMENT,
    SCALE,
    AUTHORIZE_SCALE,
    STOP,
    INCONCLUSIVE
  }
}
