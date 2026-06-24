package com.marketinghub.geralanding.wireframe.service.pending;

import java.math.BigDecimal;
import java.util.List;

/** Representa o item mínimo de pendência da etapa wireframe consumido pelo Worker AI. */
public record RecordWireframePending(
        Long experimentId,
        String jobid,
        String stageCode,
        RecordWireframeExperiment experiment,
        RecordWireframeHypothesis hypothesis,
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
    public RecordWireframePending(Long experimentId, String jobid, String stageCode, RecordWireframeExperiment experiment, RecordWireframeHypothesis hypothesis) {
        this(experimentId, jobid, stageCode, experiment, hypothesis, List.of());
    }
}
