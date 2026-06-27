package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.DossierProductUnderstandingService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberequest.DossierProductUnderstandingRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberequest.DossierProductUnderstandingRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberesponse.DossierProductUnderstandingRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.productunderstanding.service.receberesponse.DossierProductUnderstandingRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa entendimento do produto do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/moissaleslibraryworker/dossieproduto/v1/product-understanding/stage-executions")
@RequiredArgsConstructor
public class DossierProductUnderstandingController {

    private final DossierProductUnderstandingService service;

    /** Inicia manualmente a etapa para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/start")
    public void start(@PathVariable("idExterno") String idExterno) {
        service.start(idExterno);
    }


    /** Recebe o request do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public DossierProductUnderstandingRecebeRequestResponse recebeRequest(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierProductUnderstandingRecebeRequestRequest request) {
        return service.recebeRequest(idExterno, jobId, request);
    }


    /** Recebe a resposta do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public DossierProductUnderstandingRecebeResponseResponse recebeResponse(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierProductUnderstandingRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, idExterno={}, jobId={}, payload={}",
                "product-understanding",
                idExterno,
                jobId,
                request);
        return service.recebeResponse(idExterno, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierProductUnderstandingPendingResponse pending(@Valid @RequestBody DossierProductUnderstandingPendingRequest request) {
        return service.pending(request);
    }
}
