package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.targeting.TargetingAudienceType;
import com.marketinghub.targeting.TargetingRequestOrigin;
import com.marketinghub.targeting.TargetingRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Representação externa da solicitação de targeting.
 */
@Data
@Builder
public class TargetingRequestDto {
    private UUID id;

    @JsonProperty("descricao")
    private String descricao;

    @JsonProperty("idioma")
    private String idioma;

    @JsonProperty("pais")
    private String pais;

    @JsonProperty("publico_tipo")
    private TargetingAudienceType publicoTipo;

    @JsonProperty("niche_id")
    private Long nicheId;

    @JsonProperty("hypothesis_id")
    private UUID hypothesisId;

    @JsonProperty("experiment_id")
    private Long experimentId;

    private TargetingRequestStatus status;
    private TargetingRequestOrigin origin;
    private Instant createdAt;
    private Instant updatedAt;

    /** ETA estimado em segundos para o worker IA responder. */
    private Integer etaSeconds;

    private List<TargetingCandidateDto> candidates;
}
