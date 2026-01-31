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
        @JsonProperty("texto_sugerido")
        private String textoSugerido;

        @JsonProperty("tipo")
        private TargetingCandidateType tipo;

        @JsonProperty("origem")
        private String origem;

        @JsonProperty("score")
        private BigDecimal score;

        @JsonProperty("rationale")
        private String rationale;

        @JsonProperty("idioma")
        private String idioma;

        @JsonProperty("pais")
        private String pais;

        @JsonProperty("intent_tag")
        private String intentTag;
    }
}
