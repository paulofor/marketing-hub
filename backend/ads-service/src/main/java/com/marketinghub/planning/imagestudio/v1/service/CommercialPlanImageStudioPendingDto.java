package com.marketinghub.planning.imagestudio.v1.service;

import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation;
import java.util.List;

/** Responsabilidade: entregar ao recurso de Íris uma produção reservada e referências reais. */
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
    String producerExecutionId,
    TemisVisualPlaybookDto visualPlaybook) {}
