package com.marketinghub.worker.openai.core.presetdesign;

import java.util.Map;

/** Responsabilidade: representar o payload JSON validado gerado pela etapa presetdesign. */
public record PresetDesignOutput(
        Map<String, Object> payload
) {
    /** Normaliza mapas nulos para manter o contrato interno da etapa seguro. */
    public PresetDesignOutput {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}
