package com.marketinghub.mois.dossiev1.pipeline.investigationanchorbuilder;

import java.util.Map;

/** Representa a entrada funcional da etapa geração de âncoras de investigação do dossiê MOIS v1. */
public record DossierInvestigationAnchorBuilderInput(long dossierId, String workspaceId, Map<String, Object> context) {
}
