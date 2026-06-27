package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.consultaSituacao;

import java.math.BigDecimal;
import java.time.Instant;

/** Resposta auditável de situação registrada na tabela do pipeline da etapa Texto. */
public record GeraAnuncioTextoSituacaoResponse(
        Long id,
        String idExterno,
        String codigoEtapa,
        String status,
        Instant dataHora,
        String jobId,
        String request,
        String response,
        String modelo,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String descricaoErro,
        String jobIdExterno,
        String plataforma,
        String prompt,
        String schema,
        String versaoPipeline) {}
