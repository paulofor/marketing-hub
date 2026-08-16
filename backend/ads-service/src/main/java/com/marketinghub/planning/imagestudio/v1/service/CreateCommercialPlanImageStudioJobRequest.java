package com.marketinghub.planning.imagestudio.v1.service;

import com.marketinghub.planning.imagestudio.v1.CommercialPlanImageStudioOperation;
import java.util.List;

/** Responsabilidade: receber a solicitação de criação ou edição visual feita na tela do plano. */
public record CreateCommercialPlanImageStudioJobRequest(
    CommercialPlanImageStudioOperation operation,
    Long sourceAssetId,
    List<Long> referenceAssetIds,
    String prompt,
    String label,
    List<String> purposes,
    String size,
    String quality) {}
