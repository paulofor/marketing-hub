package com.marketinghub.salesvideo.dto;

import jakarta.validation.constraints.NotBlank;

/** Contrato administrativo para atualizar curadoria e gates sem alterar o adaptador técnico. */
public record UpdateSalesVideoProviderModelRequest(
    @NotBlank String recommendedUse,
    @NotBlank String lifecycleStatus,
    boolean adapterVerified,
    boolean pricingVerified,
    boolean commercialLicenseVerified,
    boolean qualityGateVerified,
    String notes) {}
