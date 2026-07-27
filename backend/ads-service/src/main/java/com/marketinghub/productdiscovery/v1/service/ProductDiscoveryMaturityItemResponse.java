package com.marketinghub.productdiscovery.v1.service;

import java.util.List;

/** Contrato de um item no ranking de maturidade comercial de oportunidades PDE. */
public record ProductDiscoveryMaturityItemResponse(
    int position,
    String niche,
    String maturity,
    String summary,
    String commercialReason,
    String recommendedAction,
    List<String> evidence,
    List<String> guardrails) {}
