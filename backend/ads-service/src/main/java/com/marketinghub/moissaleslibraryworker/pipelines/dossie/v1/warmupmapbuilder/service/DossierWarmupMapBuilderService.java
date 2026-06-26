package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.pending.DossierWarmupMapBuilderPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.pending.DossierWarmupMapBuilderPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa montagem do mapa de aquecimento do pipeline de dossiê MOIS v1. */
@Service
public class DossierWarmupMapBuilderService {

    /** Registra a intenção de iniciar a etapa para o produto informado sem executar a rotina no backend. */
    public void start(String productKey) {
        // Método reservado para criação futura da pendência canônica da etapa para a chave do produto.
    }

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierWarmupMapBuilderPendingResponse pending(DossierWarmupMapBuilderPendingRequest request) {
        return new DossierWarmupMapBuilderPendingResponse(false, List.of());
    }
}
