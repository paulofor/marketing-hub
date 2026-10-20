package com.marketinghub.salesvideo.service.reconcileNarrationCosts;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Registra consumo ou estimativa por recibos, sem receber credenciais nem autorizar gasto. */
public record ReconcileNarrationCostsRequest(
    @NotEmpty @Size(max = 50) List<Long> jobIds,
    JsonNode usage,
    boolean exclusiveUsageConfirmed,
    @NotBlank @Size(max = 1000) String evidenceReference,
    boolean estimateFromReceipts) {
  /** Preserva clientes anteriores que enviam somente consumo oficial ou reutilização. */
  public ReconcileNarrationCostsRequest(
      List<Long> jobIds,
      JsonNode usage,
      boolean exclusiveUsageConfirmed,
      String evidenceReference) {
    this(jobIds, usage, exclusiveUsageConfirmed, evidenceReference, false);
  }
}
