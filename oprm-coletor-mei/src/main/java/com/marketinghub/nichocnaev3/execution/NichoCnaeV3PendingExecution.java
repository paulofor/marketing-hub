package com.marketinghub.nichocnaev3.execution;

import java.util.Map;

/** Item pendente v3 recebido do backend para execução no OPRM. */
public record NichoCnaeV3PendingExecution(String stageExecutionId, String jobId, String cnaeCode, String inputPayload, Map<String, Object> rawPayload) {}
