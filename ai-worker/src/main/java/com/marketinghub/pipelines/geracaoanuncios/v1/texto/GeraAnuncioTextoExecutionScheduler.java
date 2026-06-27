package com.marketinghub.pipelines.geracaoanuncios.v1.texto;

import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o consumo periódico das pendências da etapa Texto do GeracaoAnuncios v1. */
public class GeraAnuncioTextoExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(GeraAnuncioTextoExecutionScheduler.class);
    private final PipelineWorker<GeraAnuncioTextoInput, GeraAnuncioTextoOutput> worker;
    private final GeraAnuncioTextoWorkerProperties properties;

    /** Recebe o worker genérico e as propriedades operacionais da etapa. */
    public GeraAnuncioTextoExecutionScheduler(
            PipelineWorker<GeraAnuncioTextoInput, GeraAnuncioTextoOutput> worker,
            GeraAnuncioTextoWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada minuto o processamento das pendências expostas pelo backend. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        if (summary.total() > 0) {
            log.info("GeracaoAnuncios v1 Texto processou pendências. total={} sucesso={} falha={}", summary.total(), summary.succeeded(), summary.failed());
        }
    }
}
