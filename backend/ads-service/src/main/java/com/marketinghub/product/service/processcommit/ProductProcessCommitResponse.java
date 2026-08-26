package com.marketinghub.product.service.processcommit;

import java.time.Instant;

/** Contrato de leitura de um commit atribuído a um produto e processo. */
public record ProductProcessCommitResponse(
    Long id,
    Long productId,
    Long processDefinitionId,
    String processCode,
    String processName,
    Integer processVersion,
    String repositoryName,
    String commitSha,
    String commitSummary,
    String commitUrl,
    String recordedBy,
    Instant recordedAt) {}
