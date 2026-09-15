package com.marketinghub.financialagent.service;

import jakarta.validation.constraints.Size;

/**
 * Responsabilidade: registrar contexto de projeção, incluindo fontes e cenários da revisão
 * financeira.
 */
public record StartRevenueProjectionRequest(@Size(max = 64000) String decisionContext) {}
