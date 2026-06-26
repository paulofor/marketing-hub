package com.marketinghub.pipelines.dossie.v1.warmupsignalextraction;

import java.util.Map;

/** Representa a entrada funcional da etapa extração de sinais de aquecimento do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionInput(long dossierId, String workspaceId, Map<String, Object> context) {
}
