package com.marketinghub.leadportal.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Contrato versionado para eventos públicos de submissão/engajamento do Lead Portal.
 */
public record LeadPortalSubmissionEngagementContractV1(
        @NotBlank(message = "Versão do contrato é obrigatória") String contractVersion,
        @NotBlank(message = "Slug do fluxo é obrigatório") String slug,
        @NotBlank(message = "ID da submissão é obrigatório") String submissionId,
        @NotNull(message = "Data/hora de submissão é obrigatória") Instant submittedAt,
        @NotNull(message = "Contato é obrigatório") @Valid Contact contato,
        String campaignCode,
        @Valid Failure failure,
        String idempotencyKey) {

    public static final String VERSION = "lead-portal-submission-engagement.v1";

    public record Contact(
            @NotBlank(message = "Nome do contato é obrigatório") String nome,
            @NotBlank(message = "E-mail do contato é obrigatório") @Email(message = "E-mail inválido") String email,
            String telefone) {
    }

    public record Failure(String reason, String detail) {
    }
}

