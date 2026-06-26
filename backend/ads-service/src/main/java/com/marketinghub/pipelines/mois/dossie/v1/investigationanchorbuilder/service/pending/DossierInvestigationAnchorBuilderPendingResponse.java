package com.marketinghub.pipelines.mois.dossie.v1.investigationanchorbuilder.service.pending;

import java.util.List;

/** Contrato de resposta do endpoint pending da etapa geração de âncoras de investigação do dossiê MOIS v1. */
public record DossierInvestigationAnchorBuilderPendingResponse(boolean claimed, List<DossierInvestigationAnchorBuilderPendingJob> jobs) {
}
