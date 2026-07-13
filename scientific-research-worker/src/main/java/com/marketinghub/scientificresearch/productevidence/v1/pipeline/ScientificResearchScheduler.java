package com.marketinghub.scientificresearch.productevidence.v1.pipeline;

import com.marketinghub.scientificresearch.config.ScientificResearchProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Controla a rotina operacional de polling do worker.
 */
@Component
public class ScientificResearchScheduler {

    private final ScientificResearchProperties properties;
    private final PipelineWorker pipelineWorker;

    /**
     * Recebe as dependências necessárias para agendar o polling.
     */
    public ScientificResearchScheduler(ScientificResearchProperties properties, PipelineWorker pipelineWorker) {
        this.properties = properties;
        this.pipelineWorker = pipelineWorker;
    }

    /**
     * Executa o polling periódico das pendências no backend.
     */
    @Scheduled(cron = "*/30 * * * * *")
    public void poll() {
        if (properties.isEnabled()) {
            pipelineWorker.pollOnce();
        }
    }
}
