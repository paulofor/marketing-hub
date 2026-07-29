package com.marketinghub.pde.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/** Representa um evento comercial da jornada PED/MUSA enviado pelo frontend. */
public record FunnelEventRequest(
        @NotBlank String productSlug,
        @NotBlank String eventType,
        String accessToken,
        String email,
        String provider,
        String source,
        String pageUrl,
        String clientIp,
        String userAgent,
        Map<String, Object> metadata
) {
    /** Mantém compatibilidade com chamadas internas que não possuem IP de requisição pública. */
    public FunnelEventRequest(
            String productSlug,
            String eventType,
            String accessToken,
            String email,
            String provider,
            String source,
            String pageUrl,
            Map<String, Object> metadata) {
        this(productSlug, eventType, accessToken, email, provider, source, pageUrl, null, null, metadata);
    }

    /** Cria uma cópia do evento contendo IP e navegador resolvidos pelo backend público. */
    public FunnelEventRequest withRequestTrafficContext(String clientIp, String userAgent) {
        return new FunnelEventRequest(
                productSlug, eventType, accessToken, email, provider, source, pageUrl, clientIp, userAgent, metadata);
    }
}
