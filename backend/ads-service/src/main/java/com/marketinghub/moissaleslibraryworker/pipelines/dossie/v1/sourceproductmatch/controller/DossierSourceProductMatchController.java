package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.DossierSourceProductMatchService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.sourceproductmatch.service.pending.DossierSourceProductMatchPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa validação de relação fonte-produto do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/source-product-match/stage-executions")
@RequiredArgsConstructor
public class DossierSourceProductMatchController {

    private final DossierSourceProductMatchService service;

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierSourceProductMatchPendingResponse pending(@Valid @RequestBody DossierSourceProductMatchPendingRequest request) {
        return service.pending(request);
    }
}
