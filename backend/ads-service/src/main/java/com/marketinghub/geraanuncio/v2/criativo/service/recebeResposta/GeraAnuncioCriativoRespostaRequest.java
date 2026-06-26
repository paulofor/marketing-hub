package com.marketinghub.geraanuncio.v2.criativo.service.recebeResposta;

import java.time.Instant;
import java.util.Map;

/** Dados auditáveis da resposta e da saída funcional da etapa Criativo. */
public record GeraAnuncioCriativoRespostaRequest(String jobId, Instant receivedAt, String status, Map<String, Object> responsePayload, Map<String, Object> structuredOutput, String error) {}
