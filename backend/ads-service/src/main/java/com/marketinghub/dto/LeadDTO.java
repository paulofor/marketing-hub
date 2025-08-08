package com.marketinghub.dto;

import com.marketinghub.model.NurtureStage;

import java.time.Instant;

/**
 * Data transfer object for incoming leads.
 */
public record LeadDTO(Long leadgenId,
                      Long instagramUserId,
                      Long adId,
                      Long campaignId,
                      Long experimentId,
                      Instant capturedAt,
                      NurtureStage nurtureStage) {
}
