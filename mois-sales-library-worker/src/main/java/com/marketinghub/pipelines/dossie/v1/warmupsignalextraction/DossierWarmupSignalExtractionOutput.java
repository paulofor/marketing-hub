package com.marketinghub.pipelines.dossie.v1.warmupsignalextraction;

import java.util.Map;

/** Representa a saída funcional da etapa extração de sinais de aquecimento do dossiê MOIS v1. */
public record DossierWarmupSignalExtractionOutput(long dossierId, String status, String businessDecision, Map<String, Object> evidence) {
}
