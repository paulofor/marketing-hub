package com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Representa o filtro de consumo de pendências da etapa fatos do produto. */
public record FatosProdutoPendingRequest(@NotBlank String workspaceId, String workerId, Integer limit) {
}
