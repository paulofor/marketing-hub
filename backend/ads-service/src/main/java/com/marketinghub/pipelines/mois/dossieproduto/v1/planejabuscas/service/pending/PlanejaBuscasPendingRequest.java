package com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.service.pending;

import jakarta.validation.constraints.NotBlank;

/** Representa o filtro de consumo de pendências da etapa planejamento de buscas. */
public record PlanejaBuscasPendingRequest(@NotBlank String workspaceId, String workerId, Integer limit) {
}
