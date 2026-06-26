package com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Representa o filtro de consumo de pendências da etapa qualificação de fontes. */
public record QualificaFontesPendingRequest(@NotBlank String workspaceId, String workerId, Integer limit) {
}
