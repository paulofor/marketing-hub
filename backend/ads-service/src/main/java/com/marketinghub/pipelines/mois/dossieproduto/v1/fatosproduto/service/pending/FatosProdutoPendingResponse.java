package com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.service.pending;

import java.util.List;

/** Representa a resposta de pendências da etapa fatos do produto para o executor. */
public record FatosProdutoPendingResponse(boolean hasWork, List<FatosProdutoPendingJob> items) {
}
