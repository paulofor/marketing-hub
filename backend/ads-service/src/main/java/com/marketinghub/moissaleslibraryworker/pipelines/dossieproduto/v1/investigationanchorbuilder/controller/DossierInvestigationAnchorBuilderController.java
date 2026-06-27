package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.DossierInvestigationAnchorBuilderService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.receberequest.DossierInvestigationAnchorBuilderRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.receberequest.DossierInvestigationAnchorBuilderRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.receberesponse.DossierInvestigationAnchorBuilderRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.investigationanchorbuilder.service.receberesponse.DossierInvestigationAnchorBuilderRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa geração de âncoras de investigação do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/moissaleslibraryworker/dossieproduto/v1/investigation-anchor-builder/stage-executions")
@RequiredArgsConstructor
public class DossierInvestigationAnchorBuilderController {

    private final DossierInvestigationAnchorBuilderService service;

    /** Inicia manualmente a etapa para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/start")
    public void start(@PathVariable("idExterno") String idExterno) {
        service.start(idExterno);
    }


    /** Recebe o request do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public DossierInvestigationAnchorBuilderRecebeRequestResponse recebeRequest(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierInvestigationAnchorBuilderRecebeRequestRequest request) {
        return service.recebeRequest(idExterno, jobId, request);
    }


    /** Recebe a resposta do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public DossierInvestigationAnchorBuilderRecebeResponseResponse recebeResponse(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierInvestigationAnchorBuilderRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, idExterno={}, jobId={}, payload={}",
                "investigation-anchor-builder",
                idExterno,
                jobId,
                request);
        return service.recebeResponse(idExterno, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierInvestigationAnchorBuilderPendingResponse pending(@Valid @RequestBody DossierInvestigationAnchorBuilderPendingRequest request) {
        return service.pending(request);
    }
}
