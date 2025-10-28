package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.model.Lead;
import com.marketinghub.leadportal.model.LeadStatus;
import java.time.Instant;
import java.util.UUID;

public record LeadResponse(
        UUID id,
        String name,
        String email,
        String notes,
        LeadStatus status,
        Instant createdAt,
        Instant completedAt,
        String result,
        String imageUrl) {

    public static LeadResponse from(Lead lead, String imageUrl) {
        return new LeadResponse(
                lead.getId(),
                lead.getName(),
                lead.getEmail(),
                lead.getNotes(),
                lead.getStatus(),
                lead.getCreatedAt(),
                lead.getCompletedAt(),
                lead.getResult(),
                imageUrl);
    }
}
