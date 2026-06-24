package com.marketinghub.metaaudience.service.requestAudience;

/** Contrato para persistir uma audiência Meta Ads já decidida pelo módulo OPRM responsável pelas regras de público. */
public record MetaAudienceRequest(Long marketNicheId, String cnaeCode, String audienceName, String segmentName,
                                  String segmentDescription, String painFocus, String desiredOutcomeFocus,
                                  String offerAngle, String filterStrategy, String facebookAdAccountId,
                                  String audienceType, String sourceType, String eligibilityStatus,
                                  Long totalContacts, Long uniqueEmails, Long estimatedContacts) {}
