package com.marketinghub.leadportal.dto;

import java.time.Instant;
import java.util.List;

public record LeadPortalEmailTemplateDto(
        String subject,
        String html,
        Instant updatedAt,
        List<LeadPortalEmailTemplatePlaceholderDto> placeholders
) {
}
