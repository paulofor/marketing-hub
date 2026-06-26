package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

import java.util.Map;

/** Contexto enviado à OpenAI para gerar personas candidatas do CNAE. */
public record PersonaCandidateGenerationRequest(String jobId, String stageExecutionId, String cnaeCode, String cnaeDescription, Map<String, Object> input) {}
