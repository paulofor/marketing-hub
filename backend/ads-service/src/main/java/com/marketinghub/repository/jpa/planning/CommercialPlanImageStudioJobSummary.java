package com.marketinghub.repository.jpa.planning;

import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus;
import java.math.BigDecimal;
import java.time.Instant;

/** Responsabilidade: representar a listagem leve de um job sem carregar payloads brutos de IA. */
public record CommercialPlanImageStudioJobSummary(
    Long id,
    Long commercialPlanId,
    Long sourceAssetId,
    Long resultAssetId,
    CommercialPlanImageStudioOperation operation,
    CommercialPlanImageStudioStatus status,
    String label,
    String prompt,
    String purposesJson,
    String size,
    String quality,
    String model,
    String playbookVersion,
    String playbookContextKey,
    BigDecimal costUsd,
    String error,
    Instant startedAt,
    Instant finishedAt,
    Instant createdAt) {}
