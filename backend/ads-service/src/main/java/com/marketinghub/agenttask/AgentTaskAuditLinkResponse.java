package com.marketinghub.agenttask;

import java.time.Instant;

/** Responsabilidade: expor um link auditável sem revelar a entidade de persistência. */
public record AgentTaskAuditLinkResponse(
    String label, String url, String accessMethod, Instant accessedAt) {}
