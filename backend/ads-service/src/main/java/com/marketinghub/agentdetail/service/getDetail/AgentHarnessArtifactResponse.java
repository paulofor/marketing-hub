package com.marketinghub.agentdetail.service.getDetail;

/** Responsabilidade: identificar um artefato versionado que participa do harness do agente. */
public record AgentHarnessArtifactResponse(
    String artifactType, String name, String version, String path, String description) {}
