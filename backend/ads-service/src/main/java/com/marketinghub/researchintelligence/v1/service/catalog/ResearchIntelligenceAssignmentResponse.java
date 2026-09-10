package com.marketinghub.researchintelligence.v1.service.catalog;

/** Expõe referência editorial cadastrada no agente e sua disponibilidade real no catálogo. */
public record ResearchIntelligenceAssignmentResponse(
    String cardId, String guidance, boolean available) {}
