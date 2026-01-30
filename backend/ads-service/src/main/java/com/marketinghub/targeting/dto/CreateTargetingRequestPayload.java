package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.targeting.TargetingAudienceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload enviado pelo cliente para solicitar hipóteses de targeting.
 */
@Data
public class CreateTargetingRequestPayload {
    @NotBlank
    @Size(max = 500)
    @JsonProperty("descricao")
    private String descricao;

    @JsonProperty("idioma")
    private String idioma;

    @JsonProperty("pais")
    private String pais;

    @JsonProperty("publico_tipo")
    private TargetingAudienceType publicoTipo;
}
