package com.marketinghub.pipelines.dossie.v1.productunderstanding;

import java.util.Map;

/** Representa a saída funcional da etapa entendimento do produto do dossiê MOIS v1. */
public record DossierProductUnderstandingOutput(long dossierId, String status, String businessDecision, Map<String, Object> evidence) {
}
