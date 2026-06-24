package com.marketinghub.geralanding.imageplanning.service.pending;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesLibraryGeraLandingInsightGateway;
import java.util.List;

/** Representa o item mínimo de pendência da etapa image planning consumido pelo Worker AI. */
public record RecordImagePlanningPending(
        Long experimentId,
        String jobid,
        String stageCode,
        RecordImagePlanningExperiment experiment,
        RecordImagePlanningHypothesis hypothesis,
        List<MoisSalesLibraryGeraLandingInsightGateway.GeraLandingReferenceInsight> geralandingReferenceInsights
) {

    /** Mantém compatibilidade com testes e consumidores antigos sem insumos MOIS. */
    public RecordImagePlanningPending(Long experimentId, String jobid, String stageCode, RecordImagePlanningExperiment experiment, RecordImagePlanningHypothesis hypothesis) {
        this(experimentId, jobid, stageCode, experiment, hypothesis, List.of());
    }
}
