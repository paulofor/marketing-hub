package com.marketinghub.experiment.directrecruitment.v1.service.submit;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Responsabilidade: receber adesão qualificada sem transportar identidade pessoal em claro. */
public record SubmitDirectRecruitmentRequest(
    @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "contactFingerprint deve ser SHA-256")
        String contactFingerprint,
    @NotBlank
        @Pattern(
            regexp =
                "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
            message = "submissionKey deve ser um UUID")
        String submissionKey,
    @NotBlank
        @Pattern(
            regexp = "^(BEAUTY_WELLNESS|CONSULTING|HOME_SERVICES|HEALTH|EDUCATION|OTHER_SERVICE)$")
        String serviceSegment,
    @NotBlank @Pattern(regexp = "^(ONE_TO_TEN|ELEVEN_TO_THIRTY|OVER_THIRTY)$")
        String weeklyConversationsRange,
    @NotNull Boolean usesWhatsapp,
    @NotNull Boolean decisionMaker,
    @NotNull Boolean wantsPersonalizedImplementation,
    @NotNull @AssertTrue Boolean consentAccepted,
    @NotBlank @Size(max = 32) String consentVersion,
    @Size(max = 100) String utmSource,
    @Size(max = 100) String utmMedium,
    @Size(max = 100) String utmCampaign,
    @Size(max = 100) String utmContent) {}
