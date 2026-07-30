package com.marketinghub.tiktokadsworker;

import jakarta.validation.constraints.NotBlank;

/** Contrato de entrada para cadastrar ou atualizar uma conta TikTok Ads. */
public record TiktokAccountRequest(
        @NotBlank String name,
        @NotBlank String advertiserId,
        String accessToken,
        String appId,
        String clientKey,
        String appSecret,
        Boolean metricsEnabled,
        Boolean publicationEnabled) {
}
