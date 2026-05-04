package com.marketinghub.geralanding;

import java.util.UUID;

public record GeraLandingPendingExecutionResponse(
        UUID idJob,
        String stageCode
) {
}
