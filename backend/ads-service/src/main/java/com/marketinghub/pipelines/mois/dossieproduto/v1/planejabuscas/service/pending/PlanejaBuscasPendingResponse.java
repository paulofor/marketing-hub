package com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.service.pending;

import java.util.List;

/** Representa a resposta de pendências da etapa planejamento de buscas para o executor. */
public record PlanejaBuscasPendingResponse(boolean hasWork, List<PlanejaBuscasPendingJob> items) {
}
