package com.marketinghub.mois.dossiev1.pipeline.productunderstanding;

/** Representa a saída funcional da etapa entendimento do produto do dossiê MOIS v1. */
public record DossierProductUnderstandingOutput(long dossierId, String status, String businessDecision) {
}
