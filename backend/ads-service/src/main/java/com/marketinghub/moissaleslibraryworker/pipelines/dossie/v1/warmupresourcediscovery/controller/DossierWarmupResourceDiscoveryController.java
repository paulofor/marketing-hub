package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.DossierWarmupResourceDiscoveryService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.receberequest.DossierWarmupResourceDiscoveryRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.receberequest.DossierWarmupResourceDiscoveryRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.receberesponse.DossierWarmupResourceDiscoveryRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupresourcediscovery.service.receberesponse.DossierWarmupResourceDiscoveryRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa descoberta de recursos de aquecimento do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/mois/dossie/v1/warmup-resource-discovery/stage-executions")
@RequiredArgsConstructor
public class DossierWarmupResourceDiscoveryController {

    private final DossierWarmupResourceDiscoveryService service;

    /** Inicia manualmente a etapa para o produto informado pela chave operacional. */
    @PostMapping("/start")
    public void start(@RequestParam("productKey") String productKey) {
        service.start(productKey);
    }


    /** Recebe o request do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/{jobId}/recebeRequest")
    public DossierWarmupResourceDiscoveryRecebeRequestResponse recebeRequest(
            @PathVariable("productKey") String productKey,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierWarmupResourceDiscoveryRecebeRequestRequest request) {
        return service.recebeRequest(productKey, jobId, request);
    }


    /** Recebe a resposta do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/{jobId}/recebeResponse")
    public DossierWarmupResourceDiscoveryRecebeResponseResponse recebeResponse(
            @PathVariable("productKey") String productKey,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierWarmupResourceDiscoveryRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, productKey={}, jobId={}, payload={}",
                "warmup-resource-discovery",
                productKey,
                jobId,
                request);
        return service.recebeResponse(productKey, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierWarmupResourceDiscoveryPendingResponse pending(@Valid @RequestBody DossierWarmupResourceDiscoveryPendingRequest request) {
        return service.pending(request);
    }
}
