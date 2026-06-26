package com.marketinghub.pipelines.mois.dossie.v1.warmupsignalextraction.controller;

import com.marketinghub.pipelines.mois.dossie.v1.warmupsignalextraction.service.DossierWarmupSignalExtractionService;
import com.marketinghub.pipelines.mois.dossie.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingRequest;
import com.marketinghub.pipelines.mois.dossie.v1.warmupsignalextraction.service.pending.DossierWarmupSignalExtractionPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa extração de sinais de aquecimento do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/warmup-signal-extraction/stage-executions")
@RequiredArgsConstructor
public class DossierWarmupSignalExtractionController {

    private final DossierWarmupSignalExtractionService service;

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierWarmupSignalExtractionPendingResponse pending(@Valid @RequestBody DossierWarmupSignalExtractionPendingRequest request) {
        return service.pending(request);
    }
}
