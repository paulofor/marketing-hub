package com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.web;

import com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.service.BackendConsolidaDossieService;
import com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.service.pending.ConsolidaDossiePendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.consolidadossie.service.pending.ConsolidaDossiePendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe os contratos internos da etapa consolidação do dossiê do pipeline dossiê do produto MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossieproduto/v1/consolida-dossie/stage-executions")
@RequiredArgsConstructor
public class BackendConsolidaDossieController {

    private final BackendConsolidaDossieService service;

    /** Entrega pendências da etapa consolidação do dossiê para o módulo executor oficial. */
    @PostMapping("/pending")
    public ConsolidaDossiePendingResponse pending(@Valid @RequestBody ConsolidaDossiePendingRequest request) {
        return service.pending(request);
    }
}
