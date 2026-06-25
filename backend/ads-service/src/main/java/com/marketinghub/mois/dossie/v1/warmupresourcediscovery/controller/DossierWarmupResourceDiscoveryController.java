package com.marketinghub.mois.dossie.v1.warmupresourcediscovery.controller;

import com.marketinghub.mois.dossie.v1.warmupresourcediscovery.service.DossierWarmupResourceDiscoveryService;
import com.marketinghub.mois.dossie.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingRequest;
import com.marketinghub.mois.dossie.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa descoberta de recursos de aquecimento do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/warmup-resource-discovery/stage-executions")
@RequiredArgsConstructor
public class DossierWarmupResourceDiscoveryController {

    private final DossierWarmupResourceDiscoveryService service;

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierWarmupResourceDiscoveryPendingResponse pending(@Valid @RequestBody DossierWarmupResourceDiscoveryPendingRequest request) {
        return service.pending(request);
    }
}
