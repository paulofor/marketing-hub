package com.marketinghub.planning.dto;

import com.marketinghub.planning.CommercialPlanVisualAssetStatus;

/** Responsabilidade: receber a decisão de aprovação ou retirada de um asset visual. */
public record UpdateCommercialPlanVisualAssetStatusRequest(
    CommercialPlanVisualAssetStatus status) {}
