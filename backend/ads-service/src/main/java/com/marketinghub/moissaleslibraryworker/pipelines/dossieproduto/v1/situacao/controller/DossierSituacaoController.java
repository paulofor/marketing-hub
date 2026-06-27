package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.controller;

import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.DossierSituacaoService;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao.DossierSituacaoRequest;
import com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao.DossierSituacaoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Expõe a consulta interna de situação das etapas do pipeline de dossiê MOIS v1. */
@RestController
@RequestMapping("/api/internal/moissaleslibraryworker/dossieproduto/v1/{etapa}/stage-executions")
@RequiredArgsConstructor
public class DossierSituacaoController {
    private final DossierSituacaoService service;

    /** Retorna registros da auditoria que existem para a etapa, identificador externo e status informados. */
    @PostMapping("/{idExterno}/situacao")
    public DossierSituacaoResponse situacao(
            @PathVariable("etapa") String etapa,
            @PathVariable("idExterno") String idExterno,
            @Valid @RequestBody DossierSituacaoRequest request) {
        return service.consultar(etapa, idExterno, request);
    }
}
