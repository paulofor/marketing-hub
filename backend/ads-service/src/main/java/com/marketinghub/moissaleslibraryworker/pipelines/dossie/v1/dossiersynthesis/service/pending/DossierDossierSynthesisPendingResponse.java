package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.service.pending;

import java.util.List;

/** Contrato de resposta do endpoint pending da etapa síntese final do dossiê do dossiê MOIS v1. */
public record DossierDossierSynthesisPendingResponse(boolean claimed, List<DossierDossierSynthesisPendingJob> jobs) {
}
