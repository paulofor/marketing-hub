package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.targeting.TargetingCandidateType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Payload recebido do AI Worker contendo candidatos de targeting.
 */
@Data
public class TargetingCandidateIngestionRequest {
    private List<CandidatePayload> candidates = new ArrayList<>();

    @Data
    public static class CandidatePayload {
        @JsonProperty("seed")
        private String seed;

        /** Campo legado aceito temporariamente. */
        @JsonProperty("texto_sugerido")
        private String legacySeed;

        @JsonProperty("seed_variants")
        private List<String> seedVariants = new ArrayList<>();

        @JsonProperty("tipo")
        private TargetingCandidateType tipo;

        @JsonProperty("origem")
        private String origem;

        @JsonProperty("score")
        private BigDecimal score;

        @JsonProperty("rationale")
        private String rationale;

        @JsonProperty("idioma_hint")
        private String idiomaHint;

        /** Campo legado aceito temporariamente. */
        @JsonProperty("idioma")
        private String idioma;

        @JsonProperty("pais")
        private String pais;

        @JsonProperty("constraints")
        private ConstraintsPayload constraints;

        @JsonProperty("intent_tag")
        private String intentTag;
    }

    @Data
    public static class ConstraintsPayload {
        private String country;

        @JsonProperty("locale")
        private String locale;
    }
}
