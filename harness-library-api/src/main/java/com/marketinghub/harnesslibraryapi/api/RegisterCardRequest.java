package com.marketinghub.harnesslibraryapi.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Representa o JSON público aceito para uma nova versão de cartão. */
public record RegisterCardRequest(
    @NotBlank @Size(max = 120) @Pattern(regexp = "^[a-z0-9][a-z0-9-]{2,119}$") String cardKey,
    @NotBlank @Size(max = 80) @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,79}$") String collection,
    @NotBlank @Size(max = 240) String title,
    @NotBlank @Size(max = 700) String finding,
    @NotBlank @Size(max = 700) String mechanism,
    @NotBlank @Size(max = 700) String commercialApplication,
    @NotBlank @Size(max = 500) String evidenceStrength,
    @NotNull LocalDate publishedOn,
    @NotNull LocalDate validUntil,
    @NotBlank @Size(max = 700) String experimentHypothesis,
    @NotBlank @Size(max = 700) String risks,
    @NotBlank @Size(max = 700) String limits,
    @NotBlank @Pattern(regexp = "^(URL|PDF|MARKDOWN|TEXT)$") String sourceKind,
    @NotBlank @Size(max = 1024) @Pattern(regexp = "^(https://|urn:|repo:|s3://)[^\\s]+$")
        String sourceUri,
    @NotBlank @Size(max = 240) String sourceTitle,
    @NotBlank @Pattern(regexp = "^[0-9a-f]{64}$") String sourceSha256) {}
