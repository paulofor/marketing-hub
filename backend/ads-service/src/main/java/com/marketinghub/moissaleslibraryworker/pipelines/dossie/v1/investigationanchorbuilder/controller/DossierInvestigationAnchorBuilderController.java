package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.DossierInvestigationAnchorBuilderService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.receberequest.DossierInvestigationAnchorBuilderRecebeRequestRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.receberequest.DossierInvestigationAnchorBuilderRecebeRequestResponse;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa geração de âncoras de investigação do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/investigation-anchor-builder/stage-executions")
@RequiredArgsConstructor
public class DossierInvestigationAnchorBuilderController {

    private final DossierInvestigationAnchorBuilderService service;

    /** Inicia manualmente a etapa para o produto informado pela chave operacional. */
    @PostMapping("/start")
    public void start(@RequestParam("productKey") String productKey) {
        service.start(productKey);
    }


    /** Recebe o request do módulo executor para a página/produto informada pela chave operacional. */
    @PostMapping("/{productKey}/recebeRequest")
    public DossierInvestigationAnchorBuilderRecebeRequestResponse recebeRequest(
            @PathVariable("productKey") String productKey,
            @Valid @RequestBody DossierInvestigationAnchorBuilderRecebeRequestRequest request) {
        return service.recebeRequest(productKey, request);
    }

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierInvestigationAnchorBuilderPendingResponse pending(@Valid @RequestBody DossierInvestigationAnchorBuilderPendingRequest request) {
        return service.pending(request);
    }
}
