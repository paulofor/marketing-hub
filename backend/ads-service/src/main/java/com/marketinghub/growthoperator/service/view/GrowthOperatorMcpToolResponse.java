package com.marketinghub.growthoperator.service.view;

import java.util.Map;

/** Responsabilidade: descrever uma ferramenta MCP disponibilizada ao Operador de Crescimento. */
public record GrowthOperatorMcpToolResponse(
    String name,
    String description,
    String accessMode,
    String dataSource,
    Map<String, String> parameters) {}
