package com.marketinghub.researchintelligence.v1.service.select;

import java.util.List;

/** Resume a seleção determinística da Biblioteca de Inteligência para um contexto de trabalho. */
public record ResearchIntelligenceSelectionResponse(
    String contractVersion,
    String contextFingerprint,
    int totalAvailableCards,
    List<ResearchIntelligenceRouteResponse> routes,
    List<String> limitations,
    com.fasterxml.jackson.databind.JsonNode expectationIntelligence) {
  /** Preserva consumidores anteriores que não pediram o protocolo de expectativas. */
  public ResearchIntelligenceSelectionResponse(
      String contractVersion,
      String contextFingerprint,
      int totalAvailableCards,
      List<ResearchIntelligenceRouteResponse> routes,
      List<String> limitations) {
    this(contractVersion, contextFingerprint, totalAvailableCards, routes, limitations, null);
  }
}
