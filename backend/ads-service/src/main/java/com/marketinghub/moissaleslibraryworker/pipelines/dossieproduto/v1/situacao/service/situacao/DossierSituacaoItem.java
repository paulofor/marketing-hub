package com.marketinghub.moissaleslibraryworker.pipelines.dossieproduto.v1.situacao.service.situacao;

import java.math.BigDecimal;
import java.time.Instant;

/** Contrato de saída responsável por expor um registro encontrado na auditoria do dossiê. */
public record DossierSituacaoItem(
        Long id,
        String idExterno,
        String codigoEtapa,
        String status,
        Instant dataHora,
        String jobId,
        String request,
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
        String versaoPipeline) {}
