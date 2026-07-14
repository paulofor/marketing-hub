package com.marketinghub.feo.fabricacaov1.pipeline;

import com.marketinghub.feo.infrastructure.config.FeoProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Controla o polling operacional da FEO no proprio modulo executor.
 */
@Component
public class FeoExecutionScheduler {

    private final PipelineWorker pipelineWorker;
    private final FeoProperties properties;

    /**
     * Recebe o worker generico e as configuracoes operacionais.
     */
    public FeoExecutionScheduler(PipelineWorker pipelineWorker, FeoProperties properties) {
        this.pipelineWorker = pipelineWorker;
        this.properties = properties;
    }

    /**
     * Consulta periodicamente o backend para processar pendencias da FEO.
     */
    @Scheduled(cron = "*/30 * * * * *")
    public void pollPendingExecutions() {
        pipelineWorker.pollOnce(properties.safePendingLimit());
    }
}
