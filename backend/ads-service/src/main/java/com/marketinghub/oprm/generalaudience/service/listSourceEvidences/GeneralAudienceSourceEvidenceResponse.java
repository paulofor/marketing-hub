package com.marketinghub.oprm.generalaudience.service.listSourceEvidences;

import java.time.Instant;

/** Contrato de saída com evidência agregada e rastreável de público geral. */
public record GeneralAudienceSourceEvidenceResponse(
        Long id,
        Long seedId,
        Long subnicheId,
        String sourceUrl,
        String sourceDomain,
        String sourceType,
        String evidenceSummary,
        Instant capturedAt
) {
}
