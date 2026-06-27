package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.pending;

import java.util.List;

/** Contrato de resposta do endpoint pending da etapa extração de sinais de aquecimento do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionPendingResponse(boolean claimed, List<DossierWarmupSignalExtractionPendingJob> jobs) {
}
