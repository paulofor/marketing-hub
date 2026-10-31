package com.marketinghub.businessprocesschain.learningcycle.v1.service.financialReview;

import java.time.Instant;

/** Responsabilidade: ler somente o parecer financeiro e sua validade, sem prompts ou auditorias. */
public record FinancialTaskSnapshot(
    Long id,
    Long processId,
    String status,
    Instant createdAt,
    Instant deliveredAt,
    Boolean accepted,
    String resultJson) {}
