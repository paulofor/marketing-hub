package com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.service.pending;

import java.time.Instant;

/** Representa um trabalho pendente da etapa planejamento de buscas entregue ao executor. */
public record PlanejaBuscasPendingJob(long stageExecutionId, long salesPageId, String workspaceId, Instant createdAt) {
}
