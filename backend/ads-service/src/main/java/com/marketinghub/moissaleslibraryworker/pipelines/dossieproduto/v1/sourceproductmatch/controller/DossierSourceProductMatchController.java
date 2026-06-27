package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.DossierSourceProductMatchService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.receberequest.DossierSourceProductMatchRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.receberequest.DossierSourceProductMatchRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.receberesponse.DossierSourceProductMatchRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.sourceproductmatch.service.receberesponse.DossierSourceProductMatchRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa validação de relação fonte-produto do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/moissaleslibraryworker/dossieproduto/v1/source-product-match/stage-executions")
@RequiredArgsConstructor
public class DossierSourceProductMatchController {

    private final DossierSourceProductMatchService service;

    /** Inicia manualmente a etapa para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/start")
    public void start(@PathVariable("idExterno") String idExterno) {
        service.start(idExterno);
    }


    /** Recebe o request do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public DossierSourceProductMatchRecebeRequestResponse recebeRequest(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierSourceProductMatchRecebeRequestRequest request) {
        return service.recebeRequest(idExterno, jobId, request);
    }


    /** Recebe a resposta do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public DossierSourceProductMatchRecebeResponseResponse recebeResponse(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierSourceProductMatchRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, idExterno={}, jobId={}, payload={}",
                "source-product-match",
                idExterno,
                jobId,
                request);
        return service.recebeResponse(idExterno, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierSourceProductMatchPendingResponse pending(@Valid @RequestBody DossierSourceProductMatchPendingRequest request) {
        return service.pending(request);
    }
}
