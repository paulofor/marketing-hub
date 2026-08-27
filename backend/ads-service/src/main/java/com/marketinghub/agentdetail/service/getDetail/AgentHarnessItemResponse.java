package com.marketinghub.agentdetail.service.getDetail;

/** Responsabilidade: expor um valor auditável que compõe uma seção do harness do agente. */
public record AgentHarnessItemResponse(
    String key, String label, String value, String description, String sourceReference) {}
