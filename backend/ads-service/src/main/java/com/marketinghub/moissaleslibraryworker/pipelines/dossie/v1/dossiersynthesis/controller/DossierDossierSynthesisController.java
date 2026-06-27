package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.service.DossierDossierSynthesisService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.service.receberequest.DossierDossierSynthesisRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.service.receberequest.DossierDossierSynthesisRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.service.pending.DossierDossierSynthesisPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.service.pending.DossierDossierSynthesisPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.service.receberesponse.DossierDossierSynthesisRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.dossiersynthesis.service.receberesponse.DossierDossierSynthesisRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa síntese final do dossiê do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/mois/dossie/v1/dossier-synthesis/stage-executions")
@RequiredArgsConstructor
public class DossierDossierSynthesisController {

    private final DossierDossierSynthesisService service;

    /** Inicia manualmente a etapa para o produto informado pela chave operacional. */
    @PostMapping("/start")
    public void start(@RequestParam("productKey") String productKey) {
        service.start(productKey);
    }


    /** Recebe o request do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/{jobId}/recebeRequest")
    public DossierDossierSynthesisRecebeRequestResponse recebeRequest(
            @PathVariable("productKey") String productKey,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierDossierSynthesisRecebeRequestRequest request) {
        return service.recebeRequest(productKey, jobId, request);
    }


    /** Recebe a resposta do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/{jobId}/recebeResponse")
    public DossierDossierSynthesisRecebeResponseResponse recebeResponse(
            @PathVariable("productKey") String productKey,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierDossierSynthesisRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, productKey={}, jobId={}, payload={}",
                "dossier-synthesis",
                productKey,
                jobId,
                request);
        return service.recebeResponse(productKey, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierDossierSynthesisPendingResponse pending(@Valid @RequestBody DossierDossierSynthesisPendingRequest request) {
        return service.pending(request);
    }
}
