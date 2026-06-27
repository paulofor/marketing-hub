package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.DossierWarmupSignalExtractionService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.receberequest.DossierWarmupSignalExtractionRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.receberequest.DossierWarmupSignalExtractionRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.receberesponse.DossierWarmupSignalExtractionRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.warmupsignalextraction.service.receberesponse.DossierWarmupSignalExtractionRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa extração de sinais de aquecimento do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/moissaleslibraryworker/dossieproduto/v1/warmup-signal-extraction/stage-executions")
@RequiredArgsConstructor
public class DossierWarmupSignalExtractionController {

    private final DossierWarmupSignalExtractionService service;

    /** Inicia manualmente a etapa para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/start")
    public void start(@PathVariable("idExterno") String idExterno) {
        service.start(idExterno);
    }


    /** Recebe o request do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public DossierWarmupSignalExtractionRecebeRequestResponse recebeRequest(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierWarmupSignalExtractionRecebeRequestRequest request) {
        return service.recebeRequest(idExterno, jobId, request);
    }


    /** Recebe a resposta do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public DossierWarmupSignalExtractionRecebeResponseResponse recebeResponse(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierWarmupSignalExtractionRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, idExterno={}, jobId={}, payload={}",
                "warmup-signal-extraction",
                idExterno,
                jobId,
                request);
        return service.recebeResponse(idExterno, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierWarmupSignalExtractionPendingResponse pending(@Valid @RequestBody DossierWarmupSignalExtractionPendingRequest request) {
        return service.pending(request);
    }
}
