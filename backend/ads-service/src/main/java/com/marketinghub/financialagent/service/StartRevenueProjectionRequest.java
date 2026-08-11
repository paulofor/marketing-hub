package com.marketinghub.financialagent.service;

import jakarta.validation.constraints.Size;

/** Responsabilidade: registrar o foco opcional de uma projeção de receita solicitada a Plutus. */
public record StartRevenueProjectionRequest(@Size(max = 4000) String decisionContext) {}
