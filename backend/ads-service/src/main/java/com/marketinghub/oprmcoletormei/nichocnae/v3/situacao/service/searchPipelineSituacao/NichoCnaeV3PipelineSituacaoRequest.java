package com.marketinghub.oprmcoletormei.nichocnae.v3.situacao.service.searchPipelineSituacao;

import java.util.List;

/** Contrato de entrada para consultar auditorias do pipeline por situação. */
public record NichoCnaeV3PipelineSituacaoRequest(List<String> status) {
}
