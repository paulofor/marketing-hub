package com.marketinghub.salesvideo.service.reconcileNarrationCosts;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Importa consumo agregado conferido pelo operador, sem receber credenciais nem autorizar gasto.
 */
public record ReconcileNarrationCostsRequest(
    @NotEmpty @Size(max = 50) List<Long> jobIds,
    JsonNode usage,
    boolean exclusiveUsageConfirmed,
    @NotBlank @Size(max = 1000) String evidenceReference) {}
