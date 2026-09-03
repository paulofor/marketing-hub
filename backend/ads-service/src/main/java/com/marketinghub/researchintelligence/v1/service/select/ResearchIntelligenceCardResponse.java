package com.marketinghub.researchintelligence.v1.service.select;

import java.time.LocalDate;

/** Representa um achado curto, rastreável e limitado derivado de um artigo versionado. */
public record ResearchIntelligenceCardResponse(
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
