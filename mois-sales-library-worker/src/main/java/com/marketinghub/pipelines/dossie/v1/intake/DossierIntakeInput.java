package com.marketinghub.pipelines.dossie.v1.intake;

import java.util.Map;

/** Representa a entrada funcional da etapa entrada inicial do dossiê MOIS v1. */
public record DossierIntakeInput(long dossierId, String workspaceId, Map<String, Object> context) {
}
