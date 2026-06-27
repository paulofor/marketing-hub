package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.DossierWarmupResourceDiscoveryService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.receberequest.DossierWarmupResourceDiscoveryRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.receberequest.DossierWarmupResourceDiscoveryRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.pending.DossierWarmupResourceDiscoveryPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.receberesponse.DossierWarmupResourceDiscoveryRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupresourcediscovery.service.receberesponse.DossierWarmupResourceDiscoveryRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa descoberta de recursos de aquecimento do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/moissaleslibraryworker/dossieproduto/v1/warmup-resource-discovery/stage-executions")
@RequiredArgsConstructor
public class DossierWarmupResourceDiscoveryController {

    private final DossierWarmupResourceDiscoveryService service;

    /** Inicia manualmente a etapa para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/start")
    public void start(@PathVariable("idExterno") String idExterno) {
        service.start(idExterno);
    }


    /** Recebe o request do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public DossierWarmupResourceDiscoveryRecebeRequestResponse recebeRequest(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierWarmupResourceDiscoveryRecebeRequestRequest request) {
        return service.recebeRequest(idExterno, jobId, request);
    }


    /** Recebe a resposta do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public DossierWarmupResourceDiscoveryRecebeResponseResponse recebeResponse(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierWarmupResourceDiscoveryRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, idExterno={}, jobId={}, payload={}",
                "warmup-resource-discovery",
                idExterno,
                jobId,
                request);
        return service.recebeResponse(idExterno, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierWarmupResourceDiscoveryPendingResponse pending(@Valid @RequestBody DossierWarmupResourceDiscoveryPendingRequest request) {
        return service.pending(request);
    }
}
