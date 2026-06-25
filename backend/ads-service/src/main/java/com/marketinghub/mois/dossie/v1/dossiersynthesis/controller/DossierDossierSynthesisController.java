package com.marketinghub.mois.dossie.v1.dossiersynthesis.controller;

import com.marketinghub.mois.dossie.v1.dossiersynthesis.service.DossierDossierSynthesisService;
import com.marketinghub.mois.dossie.v1.dossiersynthesis.service.pending.DossierDossierSynthesisPendingRequest;
import com.marketinghub.mois.dossie.v1.dossiersynthesis.service.pending.DossierDossierSynthesisPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a borda HTTP interna da etapa síntese final do dossiê do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossie/v1/dossier-synthesis/stage-executions")
@RequiredArgsConstructor
public class DossierDossierSynthesisController {

    private final DossierDossierSynthesisService service;

    /** Expõe o ponto inicial canônico de consumo da fila pelo módulo executor. */
    @PostMapping("/pending")
    public DossierDossierSynthesisPendingResponse pending(@Valid @RequestBody DossierDossierSynthesisPendingRequest request) {
        return service.pending(request);
    }
}
