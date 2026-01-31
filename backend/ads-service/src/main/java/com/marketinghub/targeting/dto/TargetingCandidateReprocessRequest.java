package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TargetingCandidateReprocessRequest {
    @JsonProperty("texto_sugerido")
    private String textoSugerido;

    private String idioma;

    private String pais;
}
