package com.marketinghub.agentdetail.service.getDetail;

/** Responsabilidade: expor um item ordenado do contrato específico de um agente. */
public record AgentDetailItemResponse(
    Long id, String name, String type, String description, Integer orderIndex) {}
