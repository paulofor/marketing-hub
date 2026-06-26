package com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Representa o filtro de consumo de pendências da etapa análise da página. */
public record AnalisePaginaPendingRequest(@NotBlank String workspaceId, String workerId, Integer limit) {
}
