package com.marketinghub.facebookadsworker.facebooktargeting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um candidato recebido do backend para resolução.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TargetingCandidatePayload(
    @JsonProperty("id") Long id,
    @JsonProperty("seed") String seed,
    @JsonProperty("texto_sugerido") String legacySeed,
    @JsonProperty("seed_variants") List<String> seedVariants,
    @JsonProperty("tipo") TargetingCandidateType tipo,
    @JsonProperty("idioma_hint") String idiomaHint,
    @JsonProperty("idioma") String idioma,
    @JsonProperty("pais") String pais,
    @JsonProperty("origem") String origem,
    @JsonProperty("score") BigDecimal score,
    @JsonProperty("rationale") String rationale,
    @JsonProperty("intent_tag") String intentTag,
    @JsonProperty("constraints") CandidateConstraints constraints
) {
    public TargetingCandidatePayload {
        if (seedVariants == null) {
            seedVariants = new ArrayList<>();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CandidateConstraints(
        @JsonProperty("country") String country,
        @JsonProperty("locale") String locale
    ) {}
}
