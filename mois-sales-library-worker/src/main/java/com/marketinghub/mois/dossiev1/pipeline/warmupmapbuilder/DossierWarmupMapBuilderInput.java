package com.marketinghub.mois.dossiev1.pipeline.warmupmapbuilder;

import java.util.Map;

/** Representa a entrada funcional da etapa montagem do mapa de aquecimento do dossiê MOIS v1. */
public record DossierWarmupMapBuilderInput(long dossierId, String workspaceId, Map<String, Object> context) {
}
