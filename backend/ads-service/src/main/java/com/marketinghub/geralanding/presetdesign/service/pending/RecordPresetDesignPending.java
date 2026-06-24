package com.marketinghub.geralanding.presetdesign.service.pending;

import java.math.BigDecimal;
import java.util.List;

/** Representa o item mínimo de pendência da etapa preset design consumido pelo Worker AI. */
public record RecordPresetDesignPending(
        Long experimentId,
        String jobid,
        String stageCode,
        RecordPresetDesignExperiment experiment,
        RecordPresetDesignHypothesis hypothesis,
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
    public RecordPresetDesignPending(Long experimentId, String jobid, String stageCode, RecordPresetDesignExperiment experiment, RecordPresetDesignHypothesis hypothesis) {
        this(experimentId, jobid, stageCode, experiment, hypothesis, List.of());
    }
}
