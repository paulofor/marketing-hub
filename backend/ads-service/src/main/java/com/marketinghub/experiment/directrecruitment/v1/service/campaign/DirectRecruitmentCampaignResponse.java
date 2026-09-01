package com.marketinghub.experiment.directrecruitment.v1.service.campaign;

import java.time.Instant;

/** Responsabilidade: apresentar estado, conteúdo, funil e bloqueios do recrutamento direto. */
public record DirectRecruitmentCampaignResponse(
    Long id,
    Long experimentId,
    String productName,
    String status,
    String contractVersion,
    String headline,
    String bodyText,
    String audienceSummary,
    String consentText,
    String consentVersion,
    String offerUrl,
    String offerCta,
    String privacyPolicyUrl,
    String publicPath,
    int targetContacts,
    long remainingContacts,
    long uniqueVisits,
    long submissions,
    long qualifiedSubmissions,
    long notQualifiedSubmissions,
    long recordedContacts,
    long connectedOrganicAccounts,
    String acquisitionStatus,
    String distributionGuidance,
    String createdBy,
    String statusChangedBy,
    String statusReason,
    Instant createdAt,
    Instant updatedAt,
    Instant activatedAt,
    Instant pausedAt,
    Instant completedAt) {}
