package com.marketinghub.scientificresearch.productevidence.v1.pipeline;

import java.util.List;
import java.util.Map;

/**
 * Retorna o resultado funcional e auditável de uma etapa executada.
 */
public record StageResult(
        StageStatus status,
        Map<String, Object> output,
        List<StageArtifact> artifacts,
        String rootCause,
        String commercialImpact,
        String recommendedAction,
        StageCode nextStageCode) {

    /**
     * Cria um resultado concluído com próxima etapa opcional.
     */
    public static StageResult completed(Map<String, Object> output, List<StageArtifact> artifacts, StageCode nextStageCode) {
        return new StageResult(StageStatus.COMPLETED, output, artifacts, null, null, null, nextStageCode);
    }

    /**
     * Cria um bloqueio funcional com causa-raiz.
     */
    public static StageResult blocked(
            Map<String, Object> output,
            List<StageArtifact> artifacts,
            String rootCause,
            String commercialImpact,
            String recommendedAction) {
        return new StageResult(StageStatus.BLOCKED, output, artifacts, rootCause, commercialImpact, recommendedAction, null);
    }
}
