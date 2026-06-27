package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.pending;

import java.util.List;

/** Contrato de resposta do endpoint pending da etapa validação de relação fonte-produto do dossiê MOIS v1. */
public record DossierSourceProductMatchPendingResponse(boolean claimed, List<DossierSourceProductMatchPendingJob> jobs) {
}
