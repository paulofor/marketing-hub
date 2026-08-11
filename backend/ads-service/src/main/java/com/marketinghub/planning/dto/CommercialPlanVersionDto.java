package com.marketinghub.planning.dto;

import java.time.Instant;

/** Responsabilidade: expor uma versão auditável do contexto comercial aos usuários e agentes. */
public record CommercialPlanVersionDto(
    Long id,
    Long commercialPlanId,
    Integer versionNumber,
    String snapshotJson,
    String changedBy,
    String changeReason,
    Instant createdAt) {}
