package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Publica pendências e contratos da etapa descoberta de recursos de aquecimento do pipeline de dossiê MOIS v1. */
@Service
public class DossierWarmupResourceDiscoveryService {

    /** Entrega trabalhos pendentes ao executor sem assumir controle operacional de execução no backend. */
    public DossierWarmupResourceDiscoveryPendingResponse pending(DossierWarmupResourceDiscoveryPendingRequest request) {
        return new DossierWarmupResourceDiscoveryPendingResponse(false, List.of());
    }
}
