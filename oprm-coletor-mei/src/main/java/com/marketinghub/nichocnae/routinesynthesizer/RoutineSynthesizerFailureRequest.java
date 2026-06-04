package com.marketinghub.nichocnae.routinesynthesizer;

/** Payload usado para notificar falha da etapa seis ao backend. */
public record RoutineSynthesizerFailureRequest(String errorMessage) {}
