package com.marketinghub.leadportal.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Lead Portal form submission payload ready to be shown in the hub.
 */
public record LeadPortalFormResponseDto(
        UUID id,
        String flowSlug,
        String flowName,
        Long experimentId,
        String experimentName,
        String name,
        String email,
        String phone,
        Instant submittedAt,
        List<LeadPortalFormResponseAnswerDto> answers) {}
