package com.marketinghub.researchintelligence.v1.service.managecard;

import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardStatus;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceSourceKind;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Expõe conteúdo, fonte, versão, roteamento e auditoria sem dados secretos. */
public record ResearchIntelligenceCardVersionResponse(
    String cardKey,
    Integer version,
    String cardId,
    ResearchIntelligenceCardStatus status,
    String effectiveStatus,
    String collection,
    List<String> routableAgents,
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
    ResearchIntelligenceSourceKind sourceKind,
    String sourceUri,
    String sourceTitle,
    String sourceSha256,
    String createdBy,
    LocalDateTime createdAt,
    String reviewSubmittedBy,
    LocalDateTime reviewSubmittedAt,
    String reviewNote,
    String activatedBy,
    LocalDateTime activatedAt,
    String activationNote,
    String archivedBy,
    LocalDateTime archivedAt,
    String archiveNote,
    LocalDateTime updatedAt) {}
