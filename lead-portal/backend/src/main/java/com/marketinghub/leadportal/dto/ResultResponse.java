package com.marketinghub.leadportal.dto;

import com.marketinghub.leadportal.model.Lead;
import com.marketinghub.leadportal.model.LeadStatus;
import java.time.Instant;
import java.util.UUID;

public record ResultResponse(UUID id, LeadStatus status, String result, Instant completedAt) {
    public static ResultResponse from(Lead lead) {
        return new ResultResponse(lead.getId(), lead.getStatus(), lead.getResult(), lead.getCompletedAt());
    }
}
