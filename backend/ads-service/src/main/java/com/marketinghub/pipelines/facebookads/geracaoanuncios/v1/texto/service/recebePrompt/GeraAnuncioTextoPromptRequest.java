package com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.texto.service.recebePrompt;

import java.time.Instant;
import java.util.Map;

/** Dados auditáveis do prompt enviado pelo AI Worker na etapa Texto. */
public record GeraAnuncioTextoPromptRequest(String jobId, Instant sentAt, String model, Map<String, Object> requestPayload) {}
