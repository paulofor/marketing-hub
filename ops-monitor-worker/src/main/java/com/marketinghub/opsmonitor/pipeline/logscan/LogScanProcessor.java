package com.marketinghub.opsmonitor.pipeline.logscan;

import com.marketinghub.opsmonitor.pipeline.StageContext;
import com.marketinghub.opsmonitor.pipeline.StageProcessor;
import java.util.List;

/** Identifica evidências de incidentes em payloads de logs operacionais. */
public class LogScanProcessor implements StageProcessor<LogScanInput, LogScanOutput> {
    private static final List<String> DEFAULT_SIGNALS = List.of("exception", "timeout", "connection refused", "openai", "facebook", "authentication", "callback failed");

    /** Procura sinais relevantes em texto bruto de log sem consultar banco de dados. */
    @Override
    public LogScanOutput process(StageContext context, LogScanInput input) {
        String payload = input.logPayload() == null ? "" : input.logPayload().toLowerCase();
        List<String> found = DEFAULT_SIGNALS.stream().filter(payload::contains).toList();
        return new LogScanOutput(input.moduleCode(), !found.isEmpty(), found);
    }
}
