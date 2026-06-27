package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.DossierWarmupMapBuilderService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.receberequest.DossierWarmupMapBuilderRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.receberequest.DossierWarmupMapBuilderRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.pending.DossierWarmupMapBuilderPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.pending.DossierWarmupMapBuilderPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.receberesponse.DossierWarmupMapBuilderRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupmapbuilder.service.receberesponse.DossierWarmupMapBuilderRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa montagem do mapa de aquecimento do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/mois/dossie/v1/warmup-map-builder/stage-executions")
@RequiredArgsConstructor
public class DossierWarmupMapBuilderController {

    private final DossierWarmupMapBuilderService service;

    /** Inicia manualmente a etapa para o produto informado pela chave operacional. */
    @PostMapping("/start")
    public void start(@RequestParam("productKey") String productKey) {
        service.start(productKey);
    }


    /** Recebe o request do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/{jobId}/recebeRequest")
    public DossierWarmupMapBuilderRecebeRequestResponse recebeRequest(
            @PathVariable("productKey") String productKey,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierWarmupMapBuilderRecebeRequestRequest request) {
        return service.recebeRequest(productKey, jobId, request);
    }


    /** Recebe a resposta do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/{jobId}/recebeResponse")
    public DossierWarmupMapBuilderRecebeResponseResponse recebeResponse(
            @PathVariable("productKey") String productKey,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierWarmupMapBuilderRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, productKey={}, jobId={}, payload={}",
                "warmup-map-builder",
                productKey,
                jobId,
                request);
        return service.recebeResponse(productKey, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierWarmupMapBuilderPendingResponse pending(@Valid @RequestBody DossierWarmupMapBuilderPendingRequest request) {
        return service.pending(request);
    }
}
