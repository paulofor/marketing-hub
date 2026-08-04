package com.marketinghub.growthoperator.service.action;

import java.util.List;

/** Responsabilidade: transportar a justificativa auditavel de uma acao do Operador. */
public record GrowthOperatorExperimentActionRequest(String reason, List<String> evidence) {}
