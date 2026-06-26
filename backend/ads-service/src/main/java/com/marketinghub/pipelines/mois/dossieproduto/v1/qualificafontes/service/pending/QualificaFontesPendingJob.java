package com.marketinghub.pipelines.mois.dossieproduto.v1.qualificafontes.service.pending;

import java.time.Instant;

/** Representa um trabalho pendente da etapa qualificação de fontes entregue ao executor. */
public record QualificaFontesPendingJob(long stageExecutionId, long salesPageId, String workspaceId, Instant createdAt) {
}
