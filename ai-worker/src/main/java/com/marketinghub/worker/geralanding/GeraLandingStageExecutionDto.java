package com.marketinghub.worker.geralanding;

import java.util.UUID;

public record GeraLandingStageExecutionDto(
        Long experimentId,
        UUID idJob,
        String stageCode
) {
}
