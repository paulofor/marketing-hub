package com.marketinghub.growthoperator.service.result;

/** Responsabilidade: receber a falha auditavel de uma execucao do worker. */
public record FailGrowthOperatorRequest(String errorMessage) {}
