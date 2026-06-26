package com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.web;

import com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.service.BackendFatosProdutoService;
import com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.service.pending.FatosProdutoPendingRequest;
import com.marketinghub.pipelines.mois.dossieproduto.v1.fatosproduto.service.pending.FatosProdutoPendingResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe os contratos internos da etapa fatos do produto do pipeline dossiê do produto MOIS v1. */
@RestController
@RequestMapping("/api/internal/mois/dossieproduto/v1/fatos-produto/stage-executions")
@RequiredArgsConstructor
public class BackendFatosProdutoController {

    private final BackendFatosProdutoService service;

    /** Entrega pendências da etapa fatos do produto para o módulo executor oficial. */
    @PostMapping("/pending")
    public FatosProdutoPendingResponse pending(@Valid @RequestBody FatosProdutoPendingRequest request) {
        return service.pending(request);
    }
}
