package com.marketinghub.geraanuncio.v2.criativo.service.recebePrompt;

import java.time.Instant;
import java.util.Map;

/** Dados auditáveis do prompt enviado pelo AI Worker na etapa Criativo. */
public record GeraAnuncioCriativoPromptRequest(String jobId, Instant sentAt, String model, Map<String, Object> requestPayload) {}
