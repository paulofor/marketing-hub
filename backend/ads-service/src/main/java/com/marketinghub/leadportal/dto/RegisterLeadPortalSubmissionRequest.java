package com.marketinghub.leadportal.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Payload enviado pelo Lead Portal sempre que um formulário é enviado com sucesso.
 */
public record RegisterLeadPortalSubmissionRequest(
        @NotBlank(message = "ID da submissão é obrigatório") String submissionId,
        Instant submittedAt) {}
