package com.marketinghub.pipelines.dossie.v1.warmupmapbuilder;

import java.util.Map;

/** Representa a saída funcional da etapa montagem do mapa de aquecimento do dossiê MOIS v1. */
public record DossierWarmupMapBuilderOutput(long dossierId, String status, String businessDecision, Map<String, Object> evidence) {
}
