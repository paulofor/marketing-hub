package com.marketinghub.worker.targetingrequest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record TargetingCandidateIngestionPayload(List<CandidatePayload> candidates) {

    public record CandidatePayload(
            @JsonProperty("seed") String seed,
            @JsonProperty("texto_sugerido") String legacySeed,
            @JsonProperty("seed_variants") List<String> seedVariants,
            @JsonProperty("tipo") TargetingCandidateType tipo,
            @JsonProperty("origem") String origem,
            @JsonProperty("score") BigDecimal score,
            @JsonProperty("rationale") String rationale,
            @JsonProperty("idioma_hint") String idiomaHint,
            @JsonProperty("pais") String country,
            @JsonProperty("intent_tag") String intentTag,
            @JsonProperty("constraints") ConstraintsPayload constraints
    ) {
        public CandidatePayload {
            if (seedVariants == null) {
                seedVariants = new ArrayList<>();
            }
        }
    }

    public record ConstraintsPayload(
            @JsonProperty("country") String country
    ) {}
}
