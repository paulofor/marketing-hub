package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Contrato de entrada responsável por informar os status pesquisados na auditoria do dossiê. */
public record DossierSituacaoRequest(@NotEmpty List<String> status) {}
