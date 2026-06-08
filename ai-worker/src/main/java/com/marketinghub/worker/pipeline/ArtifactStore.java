package com.marketinghub.worker.pipeline;

import java.util.Map;

/** Responsabilidade: abstrair a gravação ou referência dos artefatos produzidos pelas etapas do pipeline. */
public interface ArtifactStore {
    /** Registra um artefato textual ou JSON e devolve sua referência auditável. */
    StageArtifact save(String type, String name, String contentType, String content, Map<String, Object> metadata);
}
