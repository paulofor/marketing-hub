package com.marketinghub.facebookadsworker.facebooktargeting;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * Representa um candidato recebido do backend para resolução.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TargetingCandidatePayload(
    @JsonProperty("id") Long id,
    @JsonProperty("texto_sugerido") String textoSugerido,
    @JsonProperty("tipo") TargetingCandidateType tipo,
    @JsonProperty("idioma") String idioma,
    @JsonProperty("pais") String pais,
    @JsonProperty("origem") String origem,
    @JsonProperty("score") BigDecimal score,
    @JsonProperty("rationale") String rationale,
    @JsonProperty("intent_tag") String intentTag
) {}
