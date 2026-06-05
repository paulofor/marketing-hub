package com.marketinghub.nichocnae.routinequalitygate;

/** Payload usado pelo coletor para notificar falha da etapa sete ao backend. */
public record RoutineQualityGateFailureRequest(String errorMessage) {}
