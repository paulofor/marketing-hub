package com.marketinghub.metaaudience.service.internalPending;

import java.util.List;

/** Contrato interno entregue ao Facebook Ads Worker para sincronizar uma audiência na Meta. */
public record MetaAudiencePendingResponse(Long id, Long marketNicheId, String sourceCnaeCode, String audienceName,
                                          String facebookAdAccountId, List<String> emails) {}
