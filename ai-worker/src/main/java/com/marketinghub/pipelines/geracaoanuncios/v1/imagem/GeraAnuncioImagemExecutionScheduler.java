package com.marketinghub.pipelines.geracaoanuncios.v1.imagem;

import com.marketinghub.worker.pipeline.PipelineWorker;
import com.marketinghub.worker.pipeline.ProcessingSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Responsabilidade: agendar o consumo periódico das pendências da etapa Imagem do GeracaoAnuncios v1. */
public class GeraAnuncioImagemExecutionScheduler {
    private static final Logger log = LoggerFactory.getLogger(GeraAnuncioImagemExecutionScheduler.class);
    private final PipelineWorker<GeraAnuncioImagemInput, GeraAnuncioImagemOutput> worker;
    private final GeraAnuncioImagemWorkerProperties properties;

    /** Recebe o worker genérico e as propriedades operacionais da etapa. */
    public GeraAnuncioImagemExecutionScheduler(
            PipelineWorker<GeraAnuncioImagemInput, GeraAnuncioImagemOutput> worker,
            GeraAnuncioImagemWorkerProperties properties) {
        this.worker = worker;
        this.properties = properties;
    }

    /** Executa a cada minuto o processamento das pendências expostas pelo backend. */
    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        ProcessingSummary summary = worker.processPending(properties.pendingLimit());
        if (summary.total() > 0) {
            log.info("GeracaoAnuncios v1 Imagem processou pendências. total={} sucesso={} falha={}", summary.total(), summary.succeeded(), summary.failed());
        }
    }
}
