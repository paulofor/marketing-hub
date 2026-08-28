package com.marketinghub.businessprocess.execution.service.backendactivity;

/** Responsabilidade: explicar se uma atividade determinística do backend pode ser executada. */
public record BackendProductProcessActivityReadiness(boolean ready, String reason) {}
