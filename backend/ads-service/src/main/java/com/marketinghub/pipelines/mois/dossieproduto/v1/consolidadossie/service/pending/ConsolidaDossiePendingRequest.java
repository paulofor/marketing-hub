package com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Representa o filtro de consumo de pendências da etapa consolidação do dossiê. */
public record ConsolidaDossiePendingRequest(@NotBlank String workspaceId, String workerId, Integer limit) {
}
