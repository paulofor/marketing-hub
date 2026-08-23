package com.marketinghub.pde.dto;

/** Resume quantos acessos vencidos foram anonimizados pela política de retenção. */
public record PrivacyRetentionResponse(int anonymizedAccesses, String executedAt) {}
