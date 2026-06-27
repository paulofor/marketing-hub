package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao;

import java.util.List;

/** Contrato de saída responsável por listar os registros da auditoria filtrados por situação. */
public record DossierSituacaoResponse(String idExterno, String codigoEtapa, List<String> status, List<DossierSituacaoItem> registros) {}
