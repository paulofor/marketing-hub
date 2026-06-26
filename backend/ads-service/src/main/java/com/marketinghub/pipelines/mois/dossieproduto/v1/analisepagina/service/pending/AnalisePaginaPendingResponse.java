package com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.service.pending;

import java.util.List;

/** Representa a resposta de pendências da etapa análise da página para o executor. */
public record AnalisePaginaPendingResponse(boolean hasWork, List<AnalisePaginaPendingJob> items) {
}
