package com.marketinghub.leadportal.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa um envio de imagem feito no Lead Portal.
 */
public record LeadPortalSubmissionDto(
        UUID id, String flowSlug, String name, String email, String imageUrl, Instant createdAt) {}
