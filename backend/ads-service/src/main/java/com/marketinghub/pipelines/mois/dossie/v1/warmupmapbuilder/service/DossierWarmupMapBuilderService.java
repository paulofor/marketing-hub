package com.marketinghub.pipelines.mois.dossie.v1.warmupmapbuilder.service;

import com.marketinghub.pipelines.mois.dossie.v1.warmupmapbuilder.service.pending.DossierWarmupMapBuilderPendingRequest;
import com.marketinghub.pipelines.mois.dossie.v1.warmupmapbuilder.service.pending.DossierWarmupMapBuilderPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa montagem do mapa de aquecimento do pipeline de dossiê MOIS v1. */
@Service
public class DossierWarmupMapBuilderService {

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierWarmupMapBuilderPendingResponse pending(DossierWarmupMapBuilderPendingRequest request) {
        return new DossierWarmupMapBuilderPendingResponse(false, List.of());
    }
}
