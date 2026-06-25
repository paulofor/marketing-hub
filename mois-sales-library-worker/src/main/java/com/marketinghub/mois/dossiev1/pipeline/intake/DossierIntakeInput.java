package com.marketinghub.mois.dossiev1.pipeline.intake;

import java.util.Map;

/** Representa a entrada funcional da etapa entrada inicial do dossiê MOIS v1. */
public record DossierIntakeInput(long dossierId, String workspaceId, Map<String, Object> context) {
}
