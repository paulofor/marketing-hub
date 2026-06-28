package com.marketinghub.pipelines.dossie.v1.dossiersynthesis;

import java.util.List;
import java.util.Map;

/** Representa a saída funcional da etapa síntese final do dossiê do dossiê MOIS v1. */
public record DossierDossierSynthesisOutput(
        long dossierId,
        String status,
        String businessDecision,
        String finalConclusion,
        List<String> evidences,
        List<String> recommendedNextSteps,
        String qualityGateStatus,
        Map<String, Object> evidence) {
}
