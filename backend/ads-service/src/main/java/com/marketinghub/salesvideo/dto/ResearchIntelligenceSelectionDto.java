package com.marketinghub.salesvideo.dto;

import java.time.LocalDate;
import java.util.List;

/** Contrato próprio do SalesVideo para exibir e persistir pesquisa audiovisual selecionada. */
public record ResearchIntelligenceSelectionDto(
    String contractVersion,
    String contextFingerprint,
    int totalAvailableCards,
    List<Route> routes,
    List<String> limitations) {

  /** Representa a rota consultiva entregue a um único agente do fluxo audiovisual. */
  public record Route(
      String agentKey,
      String agentName,
      String purpose,
      String authority,
      String selectionReason,
      List<Card> cards) {}

  /** Representa um cartão curto cuja fonte integral continua versionada em pesquisas. */
  public record Card(
      String cardId,
      String collection,
      String title,
      String finding,
      String mechanism,
      String commercialApplication,
      String evidenceStrength,
      LocalDate publishedOn,
      LocalDate validUntil,
      String experimentHypothesis,
      String risks,
      String limits,
      String sourcePath,
      String sourceSha256,
      String evidenceKind) {}
}
