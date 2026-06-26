package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa extração de sinais de aquecimento do pipeline de dossiê MOIS v1. */
@Service
public class DossierWarmupSignalExtractionService {

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierWarmupSignalExtractionPendingResponse pending(DossierWarmupSignalExtractionPendingRequest request) {
        return new DossierWarmupSignalExtractionPendingResponse(false, List.of());
    }
}
