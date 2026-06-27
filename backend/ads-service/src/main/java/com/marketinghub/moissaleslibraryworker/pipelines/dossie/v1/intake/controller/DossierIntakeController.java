package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.DossierIntakeService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.receberequest.DossierIntakeRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.receberequest.DossierIntakeRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.pending.DossierIntakePendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.pending.DossierIntakePendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa entrada inicial do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/intake/stage-executions")
@RequiredArgsConstructor
public class DossierIntakeController {

    private final DossierIntakeService service;

    /** Inicia manualmente a etapa para o produto informado pela chave operacional. */
    @PostMapping("/start")
    public void start(@RequestParam("productKey") String productKey) {
        service.start(productKey);
    }


    /** Recebe o request do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/{jobId}/recebeRequest")
    public DossierIntakeRecebeRequestResponse recebeRequest(
            @PathVariable("productKey") String productKey,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierIntakeRecebeRequestRequest request) {
        return service.recebeRequest(productKey, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierIntakePendingResponse pending(@Valid @RequestBody DossierIntakePendingRequest request) {
        return service.pending(request);
    }
}
