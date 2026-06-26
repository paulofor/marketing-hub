package com.marketinghub.pipelines.dossie.v1.investigationanchorbuilder;

/** Representa a saída funcional da etapa geração de âncoras de investigação do dossiê MOIS v1. */
public record DossierInvestigationAnchorBuilderOutput(long dossierId, String status, String businessDecision) {
}
