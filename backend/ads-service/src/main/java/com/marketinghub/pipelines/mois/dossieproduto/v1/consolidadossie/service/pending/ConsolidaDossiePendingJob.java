package com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.service.pending;

import java.time.Instant;

/** Representa um trabalho pendente da etapa consolidação do dossiê entregue ao executor. */
public record ConsolidaDossiePendingJob(long stageExecutionId, long salesPageId, String workspaceId, Instant createdAt) {
}
