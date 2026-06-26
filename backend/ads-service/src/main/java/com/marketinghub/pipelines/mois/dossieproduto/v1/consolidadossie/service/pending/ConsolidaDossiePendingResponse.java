package com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.service.pending;

import java.util.List;

/** Representa a resposta de pendências da etapa consolidação do dossiê para o executor. */
public record ConsolidaDossiePendingResponse(boolean hasWork, List<ConsolidaDossiePendingJob> items) {
}
