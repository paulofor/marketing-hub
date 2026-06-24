package com.marketinghub.geralanding.presetdesign.service.pending;

import com.marketinghub.repository.jpa.mois.bibliotecapaginavenda.worker.v1.MoisSalesLibraryGeraLandingInsightGateway;
import java.util.List;

/** Representa o item mínimo de pendência da etapa preset design consumido pelo Worker AI. */
public record RecordPresetDesignPending(
        Long experimentId,
        String jobid,
        String stageCode,
        RecordPresetDesignExperiment experiment,
        RecordPresetDesignHypothesis hypothesis,
        List<MoisSalesLibraryGeraLandingInsightGateway.GeraLandingReferenceInsight> geralandingReferenceInsights
) {

    /** Mantém compatibilidade com testes e consumidores antigos sem insumos MOIS. */
    public RecordPresetDesignPending(Long experimentId, String jobid, String stageCode, RecordPresetDesignExperiment experiment, RecordPresetDesignHypothesis hypothesis) {
        this(experimentId, jobid, stageCode, experiment, hypothesis, List.of());
    }
}
