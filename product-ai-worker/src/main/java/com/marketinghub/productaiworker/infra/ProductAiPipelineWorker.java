package com.marketinghub.productaiworker.infra;

import com.marketinghub.productaiworker.config.ProductAiWorkerProperties;
import com.marketinghub.productaiworker.core.StageContext;
import com.marketinghub.productaiworker.core.StageProcessor;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Responsabilidade: orquestrar o consumo periódico de etapas pendentes do Product AI Worker. */
@Component
public class ProductAiPipelineWorker {
    private static final Logger log = LoggerFactory.getLogger(ProductAiPipelineWorker.class);

    private final ProductAiWorkerProperties properties;
    private final ProductAiBackendClient backendClient;
    private final Map<String, StageProcessor> processors;

    /** Inicializa o worker com catálogo de processors plugáveis. */
    public ProductAiPipelineWorker(
            ProductAiWorkerProperties properties,
            ProductAiBackendClient backendClient,
            java.util.List<StageProcessor> processors) {
        this.properties = properties;
        this.backendClient = backendClient;
        this.processors = processors.stream()
                .collect(Collectors.toMap(this::key, Function.identity()));
    }

    /** Processa pendências em intervalo fixo definido no executor. */
    @Scheduled(fixedDelayString = "60000", initialDelayString = "15000")
    public void processPending() {
        if (!properties.isEnabled()) {
            return;
        }
        for (StageContext context : backendClient.pending()) {
            StageProcessor processor = processors.get(key(context.pipelineCode(), context.stageCode()));
            if (processor == null) {
                log.warn("Nenhum processor registrado para {}/{}", context.pipelineCode(), context.stageCode());
                continue;
            }
            processor.process(context);
        }
    }

    /** Monta chave de catálogo para um processor. */
    private String key(StageProcessor processor) {
        return key(processor.pipelineCode(), processor.stageCode());
    }

    /** Monta chave de catálogo para pipeline e etapa. */
    private String key(String pipelineCode, String stageCode) {
        return pipelineCode + "/" + stageCode;
    }
}
