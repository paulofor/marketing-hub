package com.marketinghub.pipelines.dossie.v1.intake;

import java.util.Map;

/** Representa a saída funcional da etapa entrada inicial do dossiê MOIS v1. */
public record DossierIntakeOutput(long dossierId, String status, String businessDecision, Map<String, Object> evidence) {
}
