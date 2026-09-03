package com.marketinghub.researchintelligence.v1.service.select;

import java.util.List;

/** Resume a seleção determinística da Biblioteca de Inteligência para um contexto de trabalho. */
public record ResearchIntelligenceSelectionResponse(
    String contractVersion,
    String contextFingerprint,
    int totalAvailableCards,
    List<ResearchIntelligenceRouteResponse> routes,
    List<String> limitations) {}
