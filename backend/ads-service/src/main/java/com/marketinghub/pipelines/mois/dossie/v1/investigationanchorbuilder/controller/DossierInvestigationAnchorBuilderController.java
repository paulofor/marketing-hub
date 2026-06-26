package com.marketinghub.pipelines.mois.dossie.v1.investigationanchorbuilder.controller;

import com.marketinghub.pipelines.mois.dossie.v1.investigationanchorbuilder.service.DossierInvestigationAnchorBuilderService;
import com.marketinghub.pipelines.mois.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingRequest;
import com.marketinghub.pipelines.mois.dossie.v1.investigationanchorbuilder.service.pending.DossierInvestigationAnchorBuilderPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa geração de âncoras de investigação do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/investigation-anchor-builder/stage-executions")
@RequiredArgsConstructor
public class DossierInvestigationAnchorBuilderController {

    private final DossierInvestigationAnchorBuilderService service;

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierInvestigationAnchorBuilderPendingResponse pending(@Valid @RequestBody DossierInvestigationAnchorBuilderPendingRequest request) {
        return service.pending(request);
    }
}
