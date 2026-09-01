package com.marketinghub.experiment.directrecruitment.v1.service.visit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Responsabilidade: transportar uma visita pseudonimizada e sua origem de aquisição. */
public record RegisterDirectRecruitmentVisitRequest(
    @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "visitorFingerprint deve ser SHA-256")
        String visitorFingerprint,
    @Size(max = 100) String utmSource,
    @Size(max = 100) String utmMedium,
    @Size(max = 100) String utmCampaign,
    @Size(max = 100) String utmContent) {}
