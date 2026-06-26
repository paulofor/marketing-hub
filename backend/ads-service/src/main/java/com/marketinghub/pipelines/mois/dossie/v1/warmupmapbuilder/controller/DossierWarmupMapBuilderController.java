package com.marketinghub.pipelines.mois.dossie.v1.warmupmapbuilder.controller;

import com.marketinghub.pipelines.mois.dossie.v1.warmupmapbuilder.service.DossierWarmupMapBuilderService;
import com.marketinghub.pipelines.mois.dossie.v1.warmupmapbuilder.service.pending.DossierWarmupMapBuilderPendingRequest;
import com.marketinghub.pipelines.mois.dossie.v1.warmupmapbuilder.service.pending.DossierWarmupMapBuilderPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa montagem do mapa de aquecimento do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/warmup-map-builder/stage-executions")
@RequiredArgsConstructor
public class DossierWarmupMapBuilderController {

    private final DossierWarmupMapBuilderService service;

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierWarmupMapBuilderPendingResponse pending(@Valid @RequestBody DossierWarmupMapBuilderPendingRequest request) {
        return service.pending(request);
    }
}
