package com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.DossierIntakeService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.pending.DossierIntakePendingRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossie.v1.intake.service.pending.DossierIntakePendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa entrada inicial do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/intake/stage-executions")
@RequiredArgsConstructor
public class DossierIntakeController {

    private final DossierIntakeService service;

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierIntakePendingResponse pending(@Valid @RequestBody DossierIntakePendingRequest request) {
        return service.pending(request);
    }
}
