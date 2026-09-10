package com.marketinghub.researchintelligence.v1.service.catalog;

import java.util.List;

/** Descreve como um agente consulta o catálogo global sem duplicar seus cartões. */
public record ResearchIntelligenceAgentPolicyResponse(
    String agentKey,
    String agentName,
    String purpose,
    String authority,
    List<String> collections,
    int maxCardsPerContext,
    Long agentId,
    List<ResearchIntelligenceAssignmentResponse> assignments) {

  /** Preserva os produtores de contrato anteriores à curadoria explícita. */
  public ResearchIntelligenceAgentPolicyResponse(
      String agentKey,
      String agentName,
      String purpose,
      String authority,
      List<String> collections,
      int maxCardsPerContext) {
    this(agentKey, agentName, purpose, authority, collections, maxCardsPerContext, null, List.of());
  }
}
