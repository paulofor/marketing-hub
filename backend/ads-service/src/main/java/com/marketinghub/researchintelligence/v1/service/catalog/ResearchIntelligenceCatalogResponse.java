package com.marketinghub.researchintelligence.v1.service.catalog;

import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceCardResponse;
import java.time.LocalDate;
import java.util.List;

/** Expõe o catálogo global e as políticas usadas por projetos presentes e futuros. */
public record ResearchIntelligenceCatalogResponse(
    String contractVersion,
    LocalDate evaluatedOn,
    int totalCompiledCards,
    int activeCards,
    List<ResearchIntelligenceAgentPolicyResponse> agentPolicies,
    List<ResearchIntelligenceCardResponse> cards,
    List<String> limitations) {}
