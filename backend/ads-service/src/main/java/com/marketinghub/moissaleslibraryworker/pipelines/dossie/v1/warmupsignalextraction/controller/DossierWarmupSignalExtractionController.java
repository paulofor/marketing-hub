package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.DossierWarmupSignalExtractionService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.receberequest.DossierWarmupSignalExtractionRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.receberequest.DossierWarmupSignalExtractionRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa extração de sinais de aquecimento do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/warmup-signal-extraction/stage-executions")
@RequiredArgsConstructor
public class DossierWarmupSignalExtractionController {

    private final DossierWarmupSignalExtractionService service;

    /** Inicia manualmente a etapa para o produto informado pela chave operacional. */
    @PostMapping("/start")
    public void start(@RequestParam("productKey") String productKey) {
        service.start(productKey);
    }


    /** Recebe o request do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/recebeRequest")
    public DossierWarmupSignalExtractionRecebeRequestResponse recebeRequest(
            @PathVariable("productKey") String productKey,
            @Valid @RequestBody DossierWarmupSignalExtractionRecebeRequestRequest request) {
        return service.recebeRequest(productKey, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierWarmupSignalExtractionPendingResponse pending(@Valid @RequestBody DossierWarmupSignalExtractionPendingRequest request) {
        return service.pending(request);
    }
}
