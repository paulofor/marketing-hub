package com.marketinghub.targeting.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketinghub.targeting.TargetingCandidateStatus;
import com.marketinghub.targeting.TargetingCandidateType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class TargetingCandidateDto {
    private Long id;

    @JsonProperty("request_id")
    private UUID requestId;

    @JsonProperty("seed")
    private String seed;

    /** Campo legado para compatibilidade com clientes antigos. */
    @JsonProperty("texto_sugerido")
    private String legacySeed;

    @JsonProperty("seed_variants")
    private List<String> seedVariants;

    private TargetingCandidateType tipo;

    private TargetingCandidateStatus status;

    @JsonProperty("idioma_hint")
    private String idiomaHint;

    private String idioma;

    @JsonProperty("pais")
    private String pais;

    private String origem;

    @JsonProperty("intent_tag")
    private String intentTag;

    private BigDecimal score;

    private String rationale;

    @JsonProperty("rejection_reason")
    private String rejectionReason;

    private Instant createdAt;

    private Instant updatedAt;

    private List<TargetingOptionDto> options;
}
