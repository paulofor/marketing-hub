package com.marketinghub.oprm.dto;

import com.marketinghub.oprm.OprmJobStatus;

public record OprmWorkspaceOccupationSummaryDto(
        String occupationSeedRef,
        OprmJobStatus lastJobStatus,
        String lastCorrelationId,
        String lastUpdatedAt
) {
}
