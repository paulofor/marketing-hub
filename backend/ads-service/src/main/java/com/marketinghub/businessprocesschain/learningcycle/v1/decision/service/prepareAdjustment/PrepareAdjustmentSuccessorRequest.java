package com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.prepareAdjustment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Identifica a candidata e a mudança explícita de público, sem autorizar gasto ou parecer. */
public record PrepareAdjustmentSuccessorRequest(
    @Min(0) long expectedRevision,
    @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9._-]{2,63}") String productVersion,
    @Size(max = 500) String targetAudience) {
  /** Preserva clientes anteriores que alteram somente a versão, mantendo o público. */
  public PrepareAdjustmentSuccessorRequest(long expectedRevision, String productVersion) {
    this(expectedRevision, productVersion, null);
  }
}
