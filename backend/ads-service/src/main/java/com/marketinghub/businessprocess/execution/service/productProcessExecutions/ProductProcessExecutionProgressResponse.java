package com.marketinghub.businessprocess.execution.service.productProcessExecutions;

import java.time.Instant;

/** Responsabilidade: sinalizar alterações de tarefa sem reler prompts, evidências ou resultados. */
public record ProductProcessExecutionProgressResponse(
    Long taskId, String status, Instant updatedAt) {}
