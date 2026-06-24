package com.marketinghub.geralanding.wireframe.service.pending;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesLibraryGeraLandingInsightGateway;
import java.util.List;

/** Representa o item mínimo de pendência da etapa wireframe consumido pelo Worker AI. */
public record RecordWireframePending(
        Long experimentId,
        String jobid,
        String stageCode,
        RecordWireframeExperiment experiment,
        RecordWireframeHypothesis hypothesis,
        List<MoisSalesLibraryGeraLandingInsightGateway.GeraLandingReferenceInsight> geralandingReferenceInsights
) {

    /** Mantém compatibilidade com testes e consumidores antigos sem insumos MOIS. */
    public RecordWireframePending(Long experimentId, String jobid, String stageCode, RecordWireframeExperiment experiment, RecordWireframeHypothesis hypothesis) {
        this(experimentId, jobid, stageCode, experiment, hypothesis, List.of());
    }
}
