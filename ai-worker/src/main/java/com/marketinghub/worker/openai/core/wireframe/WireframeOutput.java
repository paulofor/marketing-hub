package com.marketinghub.worker.openai.core.wireframe;

import java.util.Map;

public record WireframeOutput(
        Map<String, Object> payload
) {
    public WireframeOutput {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
