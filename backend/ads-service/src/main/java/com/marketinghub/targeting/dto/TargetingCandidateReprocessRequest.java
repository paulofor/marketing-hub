package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TargetingCandidateReprocessRequest {
    @JsonProperty("seed")
    private String seed;

    /** Campo legado aceito temporariamente. */
    @JsonProperty("texto_sugerido")
    private String legacySeed;

    @JsonProperty("seed_variants")
    private List<String> seedVariants = new ArrayList<>();

    @JsonProperty("idioma_hint")
    private String idiomaHint;

    private String idioma;

    private String pais;
}
