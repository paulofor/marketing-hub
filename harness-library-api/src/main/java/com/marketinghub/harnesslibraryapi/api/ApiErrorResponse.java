package com.marketinghub.harnesslibraryapi.api;

import java.time.Instant;
import java.util.List;

/** Padroniza erros públicos sem incluir segredo, assinatura ou stack trace. */
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String requestId,
    List<String> details) {}
