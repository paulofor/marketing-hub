package com.marketinghub.pde.dto;

/** Resume o schema real do banco PDE necessário para validar métricas comerciais. */
public record DeploySchemaStatusResponse(
        boolean jdbcConfigured,
        boolean funnelEventTableExists,
        boolean funnelAnalyticsFieldsReady,
        boolean aiGuidanceTableExists,
        Integer aiGuidanceAccessTokenLength,
        boolean aiGuidanceAccessTokenReady,
        boolean operationalFailureTableExists
) {}
