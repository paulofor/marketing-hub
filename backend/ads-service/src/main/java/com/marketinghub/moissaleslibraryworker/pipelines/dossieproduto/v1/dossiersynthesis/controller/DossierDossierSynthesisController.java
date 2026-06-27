package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.DossierDossierSynthesisService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.receberequest.DossierDossierSynthesisRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.receberequest.DossierDossierSynthesisRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.pending.DossierDossierSynthesisPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.pending.DossierDossierSynthesisPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.receberesponse.DossierDossierSynthesisRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.dossiersynthesis.service.receberesponse.DossierDossierSynthesisRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa síntese final do dossiê do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/moissaleslibraryworker/dossieproduto/v1/dossier-synthesis/stage-executions")
@RequiredArgsConstructor
public class DossierDossierSynthesisController {

    private final DossierDossierSynthesisService service;

    /** Inicia manualmente a etapa para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/start")
    public void start(@PathVariable("idExterno") String idExterno) {
        service.start(idExterno);
    }


    /** Recebe o request do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public DossierDossierSynthesisRecebeRequestResponse recebeRequest(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierDossierSynthesisRecebeRequestRequest request) {
        return service.recebeRequest(idExterno, jobId, request);
    }


    /** Recebe a resposta do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public DossierDossierSynthesisRecebeResponseResponse recebeResponse(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierDossierSynthesisRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, idExterno={}, jobId={}, payload={}",
                "dossier-synthesis",
                idExterno,
                jobId,
                request);
        return service.recebeResponse(idExterno, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierDossierSynthesisPendingResponse pending(@Valid @RequestBody DossierDossierSynthesisPendingRequest request) {
        return service.pending(request);
    }
}
