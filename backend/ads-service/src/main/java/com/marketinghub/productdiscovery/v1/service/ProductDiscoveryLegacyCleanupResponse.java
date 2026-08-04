package com.marketinghub.productdiscovery.v1.service;

import java.util.List;

/** Resposta auditável da limpeza lógica de evidências artificiais legadas. */
public record ProductDiscoveryLegacyCleanupResponse(
    int archivedCycles, int archivedOpportunities, List<Long> cycleIds, String reason) {}
