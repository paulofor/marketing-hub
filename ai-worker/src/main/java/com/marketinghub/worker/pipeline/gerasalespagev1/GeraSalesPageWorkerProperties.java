package com.marketinghub.worker.pipeline.gerasalespagev1;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Responsabilidade: concentrar propriedades operacionais do worker GeraSalesPage v1. */
@Validated
@ConfigurationProperties(prefix = "gerasalespage.worker")
public record GeraSalesPageWorkerProperties(
        boolean enabled,
        @Min(1) int pendingLimit,
        @NotBlank String backendBaseUrl,
        String apiPrefix,
        @NotNull List<String> stageCodes,
        @NotNull Duration timeout,
        String serviceTier
) {
    /** Normaliza valores opcionais usados pelo ciclo operacional do GeraSalesPage v1. */
    public GeraSalesPageWorkerProperties {
        if (apiPrefix == null || apiPrefix.isBlank()) {
            apiPrefix = "/api";
        }
        if (stageCodes == null || stageCodes.isEmpty()) {
            stageCodes = List.of(
                    "sales-page-offer-brief",
                    "sales-page-wireframe",
                    "sales-page-copy",
                    "sales-page-visual-plan",
                    "sales-page-html",
                    "sales-page-checkout-quality-review",
                    "sales-page-publication-package");
        } else {
            stageCodes = List.copyOf(stageCodes);
        }
        if (timeout == null) {
            timeout = Duration.ofMinutes(30);
        }
        serviceTier = normalizeServiceTier(serviceTier);
    }

    /** Normaliza o tier OpenAI do GeraSalesPage, usando Flex por padrão operacional. */
    private static String normalizeServiceTier(String value) {
        if (value == null || value.isBlank() || "default".equalsIgnoreCase(value.trim())) {
            return "flex";
        }
        String normalized = value.trim().toLowerCase();
        return "standard".equals(normalized) ? "default" : normalized;
    }
}
