package com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.service.pending;

import java.time.Instant;

/** Representa um trabalho pendente da etapa fatos do produto entregue ao executor. */
public record FatosProdutoPendingJob(long stageExecutionId, long salesPageId, String workspaceId, Instant createdAt) {
}
