package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.DossierProductUnderstandingService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.receberequest.DossierProductUnderstandingRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.receberequest.DossierProductUnderstandingRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.productunderstanding.service.pending.DossierProductUnderstandingPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa entendimento do produto do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/product-understanding/stage-executions")
@RequiredArgsConstructor
public class DossierProductUnderstandingController {

    private final DossierProductUnderstandingService service;

    /** Inicia manualmente a etapa para o produto informado pela chave operacional. */
    @PostMapping("/start")
    public void start(@RequestParam("productKey") String productKey) {
        service.start(productKey);
    }


    /** Recebe o request do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/{jobId}/recebeRequest")
    public DossierProductUnderstandingRecebeRequestResponse recebeRequest(
            @PathVariable("productKey") String productKey,
            @PathVariable("jobId") String jobId,
            @Valid @RequestBody DossierProductUnderstandingRecebeRequestRequest request) {
        return service.recebeRequest(productKey, jobId, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierProductUnderstandingPendingResponse pending(@Valid @RequestBody DossierProductUnderstandingPendingRequest request) {
        return service.pending(request);
    }
}
