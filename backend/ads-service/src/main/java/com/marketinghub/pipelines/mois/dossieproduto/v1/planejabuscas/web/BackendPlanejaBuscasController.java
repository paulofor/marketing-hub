package com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.web;

import com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.service.BackendPlanejaBuscasService;
import com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.service.pending.PlanejaBuscasPendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.planejabuscas.service.pending.PlanejaBuscasPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe os contratos internos da etapa planejamento de buscas do pipeline dossiê do produto MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossieproduto/v1/planeja-buscas/stage-executions")
@RequiredArgsConstructor
public class BackendPlanejaBuscasController {

    private final BackendPlanejaBuscasService service;

    /** Entrega pendências da etapa planejamento de buscas para o módulo executor oficial. */
    @PostMapping("/pending")
    public PlanejaBuscasPendingResponse pending(@Valid @RequestBody PlanejaBuscasPendingRequest request) {
        return service.pending(request);
    }
}
