package com.marketinghub.experiment.directcontact.v1;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Responsabilidade: receber a prova mínima de um contato direto sem transportar dado pessoal. */
public record RegisterExperimentDirectContactRequest(
    @NotBlank
        @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "contactFingerprint deve ser SHA-256")
        String contactFingerprint,
    @NotBlank @Size(max = 500) String consentEvidenceReference,
    @NotNull Instant consentRecordedAt,
    @NotNull Instant contactedAt,
    @NotNull @AssertTrue Boolean audienceFitConfirmed,
    @NotBlank @Size(max = 100) String recordedBy) {}
