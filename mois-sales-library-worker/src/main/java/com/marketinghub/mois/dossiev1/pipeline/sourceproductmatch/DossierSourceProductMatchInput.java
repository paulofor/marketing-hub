package com.marketinghub.mois.dossiev1.pipeline.sourceproductmatch;

import java.util.Map;

/** Representa a entrada funcional da etapa validação de relação fonte-produto do dossiê MOIS v1. */
public record DossierSourceProductMatchInput(long dossierId, String workspaceId, Map<String, Object> context) {
}
