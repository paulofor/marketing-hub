package com.marketinghub.pipelines.aiworker.geracaoanuncios.v1.texto.service.recebeResposta;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Dados auditáveis da resposta e da saída funcional da etapa Texto. */
public record GeraAnuncioTextoRespostaRequest(
        String jobId,
        Instant receivedAt,
        String status,
        Object response,
        Map<String, Object> responsePayload,
        Map<String, Object> structuredOutput,
        String error,
        String descricaoErro,
        Long quantidadeTokenEntrada,
        Long quantidadeTokenSaida,
        BigDecimal custo,
        String modelo) {}
