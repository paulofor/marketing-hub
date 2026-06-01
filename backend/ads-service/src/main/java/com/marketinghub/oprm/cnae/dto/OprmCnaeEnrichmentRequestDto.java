package com.marketinghub.oprm.cnae.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO de publicação de enriquecimento e candidatos de nicho gerados pelo módulo OPRM.
 */
public record OprmCnaeEnrichmentRequestDto(
        @NotBlank String cnaeCode,
        @NotBlank String enrichmentCycleId,
        String routineSignals,
        String painSignals,
        String mechanismSignals,
        String proofSignals,
        String offerSignals,
        String sourceSummary,
        @Valid @NotEmpty List<OprmNicheCandidateRequestDto> candidates) {}
