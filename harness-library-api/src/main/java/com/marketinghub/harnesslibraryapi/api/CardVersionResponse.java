package com.marketinghub.harnesslibraryapi.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Expõe a versão editorial devolvida pelo backend canônico. */
public record CardVersionResponse(
    String cardKey,
    Integer version,
    String cardId,
    String status,
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
    String sourceKind,
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
