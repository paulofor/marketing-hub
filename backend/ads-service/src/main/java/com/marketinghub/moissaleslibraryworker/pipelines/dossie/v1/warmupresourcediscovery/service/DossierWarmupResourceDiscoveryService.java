package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa descoberta de recursos de aquecimento do pipeline de dossiê MOIS v1. */
@Service
public class DossierWarmupResourceDiscoveryService {

    /** Registra a intenção de iniciar a etapa para o produto informado sem executar a rotina no backend. */
    public void start(String productKey) {
        // Método reservado para criação futura da pendência canônica da etapa para a chave do produto.
    }

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierWarmupResourceDiscoveryPendingResponse pending(DossierWarmupResourceDiscoveryPendingRequest request) {
        return new DossierWarmupResourceDiscoveryPendingResponse(false, List.of());
    }
}
