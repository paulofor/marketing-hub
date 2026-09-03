package com.marketinghub.researchintelligence.v1.service.select;

import java.util.List;

/** Entrega a um agente somente os cartões aderentes à sua responsabilidade. */
public record ResearchIntelligenceRouteResponse(
    String agentKey,
    String agentName,
    String purpose,
    String authority,
    String selectionReason,
    List<ResearchIntelligenceCardResponse> cards) {}
