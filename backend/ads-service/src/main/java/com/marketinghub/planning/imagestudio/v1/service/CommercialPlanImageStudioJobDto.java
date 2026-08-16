package com.marketinghub.planning.imagestudio.v1.service;

import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation;
import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Responsabilidade: expor o andamento auditável de uma produção visual do plano comercial. */
public record CommercialPlanImageStudioJobDto(
    Long id,
    Long commercialPlanId,
    Long sourceAssetId,
    Long resultAssetId,
    CommercialPlanImageStudioOperation operation,
    CommercialPlanImageStudioStatus status,
    String label,
    String prompt,
    List<String> purposes,
    String size,
    String quality,
    String model,
    BigDecimal costUsd,
    String error,
    Instant startedAt,
    Instant finishedAt,
    Instant createdAt) {}
