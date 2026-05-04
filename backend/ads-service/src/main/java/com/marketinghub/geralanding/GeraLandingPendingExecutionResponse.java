package com.marketinghub.geralanding;

import java.util.UUID;

public record GeraLandingPendingExecutionResponse(
        Long experimentId,
        UUID idJob,
        String stageCode
) {
}
