package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.pending;

import java.util.List;

/** Contrato de resposta do endpoint pending da etapa entendimento do produto do dossiê MOIS v1. */
public record DossierProductUnderstandingPendingResponse(boolean claimed, List<DossierProductUnderstandingPendingJob> jobs) {
}
