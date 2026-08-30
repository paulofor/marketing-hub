package com.marketinghub.businessprocess.execution.service.predecessor;

/** Responsabilidade: explicar se as atividades anteriores já liberaram uma atividade do produto. */
public record ProductProcessActivityPredecessorReadiness(boolean ready, String reason) {}
