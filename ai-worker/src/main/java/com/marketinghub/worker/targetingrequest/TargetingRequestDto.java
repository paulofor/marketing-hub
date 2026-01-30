package com.marketinghub.worker.targetingrequest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record TargetingRequestDto(
        UUID id,
        @JsonProperty("descricao") String descricao,
        @JsonProperty("idioma") String idioma,
        @JsonProperty("pais") String pais,
        @JsonProperty("publico_tipo") TargetingAudienceType publicoTipo,
        String status,
        Integer etaSeconds
) {
    public String localeOrDefault() {
        if (idioma == null || idioma.isBlank()) {
            return "pt_BR";
        }
        return idioma.replace('-', '_');
    }

    public String countryOrDefault() {
        if (pais == null || pais.isBlank()) {
            return "BR";
        }
        return pais.toUpperCase();
    }

    public TargetingAudienceType audienceOrDefault() {
        return publicoTipo != null ? publicoTipo : TargetingAudienceType.PROSPECT;
    }
}
