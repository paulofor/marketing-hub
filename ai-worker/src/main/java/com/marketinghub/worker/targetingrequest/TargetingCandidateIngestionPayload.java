package com.marketinghub.worker.targetingrequest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record TargetingCandidateIngestionPayload(List<CandidatePayload> candidates) {

    public record CandidatePayload(
            @JsonProperty("texto_sugerido") String textoSugerido,
            @JsonProperty("tipo") TargetingCandidateType tipo,
            @JsonProperty("origem") String origem,
            @JsonProperty("score") BigDecimal score,
            @JsonProperty("rationale") String rationale,
            @JsonProperty("idioma") String idioma,
            @JsonProperty("intent_tag") String intentTag
    ) {
    }
}
