package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.pending;

import java.util.List;

/** Contrato de resposta do endpoint pending da etapa entrada inicial do dossiê MOIS v1. */
public record DossierIntakePendingResponse(boolean claimed, List<DossierIntakePendingJob> jobs) {
}
