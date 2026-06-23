package com.marketinghub.opsmonitor.pipeline.logscan;

/** Entrada textual usada para buscar sinais operacionais em logs. */
public record LogScanInput(String moduleCode, String logPayload) {}
