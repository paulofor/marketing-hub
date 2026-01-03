package com.marketinghub.marketresearch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public record MarketResearchRequest(
        @NotBlank(message = "Informe o problema ou hipótese de pesquisa")
        String query,
        @Size(max = 10, message = "Limite de 10 fontes externas")
        List<String> sources,
        String analysisObjective
) {
    public List<String> normalizedSources() {
        if (sources == null) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String source : sources) {
            if (source != null && !source.isBlank()) {
                normalized.add(source.trim());
            }
        }
        return normalized;
    }
}
