package com.marketinghub.worker.geralanding;

import java.util.Map;

public record GeraLandingPromptContext(
        Long experimentId,
        String idJob,
        String stageCode,
        Map<String, Object> dados
) {
}
