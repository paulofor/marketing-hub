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
        Map<String, Object> metadata
) {}
