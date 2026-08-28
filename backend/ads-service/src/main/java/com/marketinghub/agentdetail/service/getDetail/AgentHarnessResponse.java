package com.marketinghub.agentdetail.service.getDetail;

import java.util.List;

/** Responsabilidade: expor o manifesto completo e seguro do harness operacional de um agente. */
public record AgentHarnessResponse(
    String status,
    String contractVersion,
    String sourceReference,
    String sensitiveValuesPolicy,
    List<AgentHarnessSectionResponse> sections,
    List<AgentHarnessArtifactResponse> artifacts,
    List<AgentBehaviorFileResponse> behaviorFiles) {

  /** Representa explicitamente um agente ainda sem manifesto de harness cadastrado. */
  public static AgentHarnessResponse notRegistered(
      String contractVersion, String sourceReference, String sensitiveValuesPolicy) {
    return new AgentHarnessResponse(
        "NOT_REGISTERED",
        contractVersion,
        sourceReference,
        sensitiveValuesPolicy,
        List.of(),
        List.of(),
        List.of());
  }
}
