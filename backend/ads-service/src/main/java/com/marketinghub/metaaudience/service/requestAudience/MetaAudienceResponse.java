package com.marketinghub.metaaudience.service.requestAudience;

/** Contrato de resposta com o estado da audiência Meta Ads criada no backend. */
public record MetaAudienceResponse(Long id, Long marketNicheId, String audienceName, String eligibilityStatus,
                                   long totalContacts, long uniqueEmails) {}
