package com.marketinghub.oprm.generalaudience.service.createSourceEvidence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Contrato de entrada para registrar evidência agregada de público geral sem dados pessoais. */
public record CreateGeneralAudienceSourceEvidenceRequest(
        Long subnicheId,
        @Size(max = 1024) String sourceUrl,
        @Size(max = 191) String sourceDomain,
        @Size(max = 64) String sourceType,
        @NotBlank String evidenceSummary,
        Instant capturedAt
) {
}
