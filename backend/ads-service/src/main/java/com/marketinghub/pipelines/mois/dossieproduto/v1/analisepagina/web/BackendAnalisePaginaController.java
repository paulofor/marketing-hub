package com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.web;

import com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.service.BackendAnalisePaginaService;
import com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.service.pending.AnalisePaginaPendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.analisepagina.service.pending.AnalisePaginaPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe os contratos internos da etapa análise da página do pipeline dossiê do produto MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossieproduto/v1/analise-pagina/stage-executions")
@RequiredArgsConstructor
public class BackendAnalisePaginaController {

    private final BackendAnalisePaginaService service;

    /** Entrega pendências da etapa análise da página para o módulo executor oficial. */
    @PostMapping("/pending")
    public AnalisePaginaPendingResponse pending(@Valid @RequestBody AnalisePaginaPendingRequest request) {
        return service.pending(request);
    }
}
