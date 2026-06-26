package com.marketinghub.pipelines.facebookads.geracaoanuncios.v1.imagem.service.recebePrompt;

import java.time.Instant;
import java.util.Map;

/** Dados auditáveis do prompt enviado pelo AI Worker na etapa Imagem. */
public record GeraAnuncioImagemPromptRequest(String jobId, Instant sentAt, String model, Map<String, Object> requestPayload) {}
