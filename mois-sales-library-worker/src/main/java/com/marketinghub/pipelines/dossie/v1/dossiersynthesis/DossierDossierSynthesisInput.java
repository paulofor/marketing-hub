package com.marketinghub.pipelines.dossie.v1.dossiersynthesis;

import java.util.Map;

/** Representa a entrada funcional da etapa síntese final do dossiê do dossiê MOIS v1. */
public record DossierDossierSynthesisInput(long dossierId, String workspaceId, Map<String, Object> context) {
}
