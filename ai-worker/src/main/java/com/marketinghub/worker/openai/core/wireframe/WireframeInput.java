package com.marketinghub.worker.openai.core.wireframe;

import java.util.Map;

public record WireframeInput(
        Long experimentId,
        String stageCode,
        String idJob,
        Map<String, Object> promptData
) {
    public WireframeInput {
        promptData = promptData == null ? Map.of() : Map.copyOf(promptData);
    }
}
