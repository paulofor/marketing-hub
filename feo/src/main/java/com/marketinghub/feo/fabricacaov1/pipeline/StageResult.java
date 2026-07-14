package com.marketinghub.feo.fabricacaov1.pipeline;

import java.util.List;
import java.util.Map;

/**
 * Consolida a saida funcional, artefatos e metricas de uma etapa.
 */
public record StageResult<O>(
        StageStatus status,
        O output,
        List<StageArtifact> artifacts,
        Map<String, Object> metrics,
        String blockReason,
        StageCode nextStageCode) {

    /**
     * Cria resultado concluido sem proxima etapa automatica.
     */
    public static <O> StageResult<O> completed(O output, List<StageArtifact> artifacts, Map<String, Object> metrics) {
        return new StageResult<>(StageStatus.COMPLETED, output, artifacts, metrics, null, null);
    }

    /**
     * Cria resultado concluido apontando a proxima etapa contratada.
     */
    public static <O> StageResult<O> completedWithNext(
            O output,
            List<StageArtifact> artifacts,
            Map<String, Object> metrics,
            StageCode nextStageCode) {
        return new StageResult<>(StageStatus.COMPLETED, output, artifacts, metrics, null, nextStageCode);
    }

    /**
     * Cria resultado bloqueado com causa funcional explicita.
     */
    public static <O> StageResult<O> blocked(String reason, List<StageArtifact> artifacts) {
        return new StageResult<>(StageStatus.BLOCKED, null, artifacts, Map.of(), reason, null);
    }
}
