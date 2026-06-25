package com.marketinghub.mois.dossiev1.pipeline.productunderstanding;

import java.util.Map;

/** Representa a entrada funcional da etapa entendimento do produto do dossiê MOIS v1. */
public record DossierProductUnderstandingInput(long dossierId, String workspaceId, Map<String, Object> context) {
}
