package com.marketinghub.planning.imagestudio.v1.service;

import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation;
import java.util.List;

/** Responsabilidade: entregar a Têmis uma produção reservada e suas referências reais. */
public record CommercialPlanImageStudioPendingDto(
    Long jobId,
    Long commercialPlanId,
    CommercialPlanImageStudioOperation operation,
    String prompt,
    String label,
    List<String> purposes,
    String size,
    String quality,
    List<String> referenceImageUrls,
    String producerExecutionId) {}
