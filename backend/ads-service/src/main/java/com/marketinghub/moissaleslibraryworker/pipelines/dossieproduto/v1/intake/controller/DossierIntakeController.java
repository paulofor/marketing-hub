package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.DossierIntakeService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.receberequest.DossierIntakeRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.receberequest.DossierIntakeRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.pending.DossierIntakePendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.pending.DossierIntakePendingResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.receberesponse.DossierIntakeRecebeResponseRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.intake.service.receberesponse.DossierIntakeRecebeResponseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa entrada inicial do pipeline de dossiê MOIS v1. */
@RestController
@Slf4j
@RequestMapping("/api/internal/moissaleslibraryworker/dossieproduto/v1/intake/stage-executions")
@RequiredArgsConstructor
public class DossierIntakeController {

    private final DossierIntakeService service;

    /** Inicia manualmente a etapa para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/start")
    public void start(@PathVariable("idExterno") String idExterno) {
        service.start(idExterno);
    }


    /** Recebe o request do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeRequest")
    public DossierIntakeRecebeRequestResponse recebeRequest(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierIntakeRecebeRequestRequest request) {
        return service.recebeRequest(idExterno, jobId, request);
    }


    /** Recebe a resposta do módulo executor para o identificador externo informado na URL. */
    @PostMapping("/{idExterno}/{jobId}/recebeResponse")
    public DossierIntakeRecebeResponseResponse recebeResponse(
            @PathVariable("idExterno") String idExterno,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierIntakeRecebeResponseRequest request) {
        log.info(
                "Recebendo response do dossiê MOIS v1: etapa={}, idExterno={}, jobId={}, payload={}",
                "intake",
                idExterno,
                jobId,
                request);
        return service.recebeResponse(idExterno, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierIntakePendingResponse pending(@Valid @RequestBody DossierIntakePendingRequest request) {
        return service.pending(request);
    }
}
