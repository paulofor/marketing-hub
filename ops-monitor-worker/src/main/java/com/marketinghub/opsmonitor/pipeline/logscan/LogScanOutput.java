package com.marketinghub.opsmonitor.pipeline.logscan;

import java.util.List;

/** Resultado da análise de sinais relevantes em logs operacionais. */
public record LogScanOutput(String moduleCode, boolean incidentSignalFound, List<String> signals) {}
