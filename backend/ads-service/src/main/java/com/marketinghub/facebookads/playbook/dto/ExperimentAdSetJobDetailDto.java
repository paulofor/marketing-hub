package com.marketinghub.facebookads.playbook.dto;

import java.util.List;

/**
 * Detailed view of a job plus its recorded API calls.
 */
public record ExperimentAdSetJobDetailDto(
        ExperimentAdSetJobDto job,
        String payload,
        String resultPayload,
        List<ExperimentAdSetJobApiLogDto> apiLogs
) {
}
