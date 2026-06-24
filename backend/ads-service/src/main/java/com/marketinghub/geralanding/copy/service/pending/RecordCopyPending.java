package com.marketinghub.geralanding.copy.service.pending;

import java.math.BigDecimal;
import java.util.List;

/** Representa o item mínimo de pendência da etapa copy consumido pelo Worker AI. */
public record RecordCopyPending(
        Long experimentId,
        String jobid,
        String idJob,
        String stageCode,
        String status,
        RecordCopyExperiment experiment,
        RecordCopyHypothesis hypothesis,
        List<GeraLandingReferenceInsight> geralandingReferenceInsights
) {

    /** Representa uma referência vencedora persistida pelo MOIS e consumida pelo GeraLanding via banco. */
    public record GeraLandingReferenceInsight(
            Long pageId,
            String urlCanonical,
            String title,
            BigDecimal scoreTotal,
            Object wireframeInsight,
            Object copyInsight,
            Object imagePromptInsight,
            Object designPresetInsight
    ) {
    }

    /** Mantém compatibilidade com testes e consumidores antigos sem insumos MOIS. */
    public RecordCopyPending(Long experimentId, String jobid, String idJob, String stageCode, String status, RecordCopyExperiment experiment, RecordCopyHypothesis hypothesis) {
        this(experimentId, jobid, idJob, stageCode, status, experiment, hypothesis, List.of());
    }
}
