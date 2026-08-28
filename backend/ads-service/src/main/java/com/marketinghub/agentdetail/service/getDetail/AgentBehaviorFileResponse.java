package com.marketinghub.agentdetail.service.getDetail;

/**
 * Responsabilidade: expor uma fonte versionada que define ou restringe o comportamento do agente.
 */
public record AgentBehaviorFileResponse(
    String behaviorType,
    String name,
    String version,
    String path,
    String description,
    String mediaType,
    String sha256,
    String content) {}
