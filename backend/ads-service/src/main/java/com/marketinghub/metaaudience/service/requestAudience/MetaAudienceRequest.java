package com.marketinghub.metaaudience.service.requestAudience;

/** Contrato para solicitar criação de audiência Meta Ads a partir de emails OPRM por CNAE. */
public record MetaAudienceRequest(Long marketNicheId, String cnaeCode, String segmentName, String segmentDescription,
                                  String painFocus, String desiredOutcomeFocus, String offerAngle,
                                  String filterStrategy, String facebookAdAccountId) {}
