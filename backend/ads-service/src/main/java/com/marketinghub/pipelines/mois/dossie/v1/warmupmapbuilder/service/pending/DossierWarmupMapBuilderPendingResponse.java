package com.marketinghub.pipelines.mois.dossie.v1.warmupmapbuilder.service.pending;

import java.util.List;

/** Contrato de resposta do endpoint pending da etapa montagem do mapa de aquecimento do dossiê MOIS v1. */
public record DossierWarmupMapBuilderPendingResponse(boolean claimed, List<DossierWarmupMapBuilderPendingJob> jobs) {
}
