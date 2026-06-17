package com.marketinghub.nichocnae.routinequalitygate;

/** Payload usado pelo coletor para registrar no backend um novo ciclo automático decidido pelo módulo externo. */
public record RoutineQualityReprocessRequest(String triggerSource) {}
