package com.marketinghub.oprmcoletormei.nichocnae.v3.situacao.service.searchPipelineSituacao;

import java.math.BigDecimal;
import java.time.Instant;

/** Contrato de saída com uma auditoria do pipeline NichoCNAE encontrada por situação. */
public record NichoCnaeV3PipelineSituacaoResponse(
        String idExterno,
        String codigoEtapa,
        String status,
        Instant dataHora,
        String jobId,
        String request,
        String requestInput,
        String response,
        String respostaFinal,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        String modelo,
        BigDecimal custo,
        String descricaoErro,
        String jobIdExterno,
        String plataforma,
        String prompt,
        String schema,
        String versaoPipeline) {
}
