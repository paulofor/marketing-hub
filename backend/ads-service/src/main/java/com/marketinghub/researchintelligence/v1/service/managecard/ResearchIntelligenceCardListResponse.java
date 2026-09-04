package com.marketinghub.researchintelligence.v1.service.managecard;

import java.util.List;

/** Agrupa uma página limitada de versões para consulta operacional. */
public record ResearchIntelligenceCardListResponse(
    int returnedItems, List<ResearchIntelligenceCardVersionResponse> items) {}
