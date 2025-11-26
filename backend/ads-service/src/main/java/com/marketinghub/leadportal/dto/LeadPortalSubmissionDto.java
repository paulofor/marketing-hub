package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.FlowSubmissionImagePackageStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa um pacote de imagens oriundo do Lead Portal que ainda precisa entrar no
 * pipeline de criação.
 */
public record LeadPortalSubmissionDto(
        Long id,
        UUID submissionId,
        String flowSlug,
        String name,
        String email,
        String phone,
        String prompt,
        FlowSubmissionImagePackageStatus status,
        Instant createdAt) {}
