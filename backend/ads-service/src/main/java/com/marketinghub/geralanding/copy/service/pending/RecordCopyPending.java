package com.marketinghub.geralanding.copy.service.pending;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesLibraryGeraLandingInsightGateway;
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
        List<MoisSalesLibraryGeraLandingInsightGateway.GeraLandingReferenceInsight> geralandingReferenceInsights
) {

    /** Mantém compatibilidade com testes e consumidores antigos sem insumos MOIS. */
    public RecordCopyPending(Long experimentId, String jobid, String idJob, String stageCode, String status, RecordCopyExperiment experiment, RecordCopyHypothesis hypothesis) {
        this(experimentId, jobid, idJob, stageCode, status, experiment, hypothesis, List.of());
    }
}
