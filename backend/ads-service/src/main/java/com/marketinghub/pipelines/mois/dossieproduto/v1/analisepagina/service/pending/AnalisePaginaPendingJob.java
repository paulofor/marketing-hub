package com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.service.pending;

import java.time.Instant;

/** Representa um trabalho pendente da etapa análise da página entregue ao executor. */
public record AnalisePaginaPendingJob(long stageExecutionId, long salesPageId, String workspaceId, Instant createdAt) {
}
