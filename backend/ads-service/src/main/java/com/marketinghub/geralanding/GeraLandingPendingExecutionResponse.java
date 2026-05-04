package com.marketinghub.geralanding;

public record GeraLandingPendingExecutionResponse(
        Long experimentId,
        String idJob,
        String stageCode
) {
}
