package com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.service.pending;

import java.util.List;

/** Representa a resposta de pendências da etapa qualificação de fontes para o executor. */
public record QualificaFontesPendingResponse(boolean hasWork, List<QualificaFontesPendingJob> items) {
}
