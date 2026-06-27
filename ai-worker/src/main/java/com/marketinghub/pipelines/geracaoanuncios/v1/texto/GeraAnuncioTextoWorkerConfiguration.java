package com.marketinghub.pipelines.geracaoanuncios.v1.texto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.pipeline.InMemoryArtifactStore;
import com.marketinghub.worker.pipeline.PipelineWorker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa Texto do GeracaoAnuncios v1. */
@Configuration
@EnableConfigurationProperties(GeraAnuncioTextoWorkerProperties.class)
@ConditionalOnProperty(prefix = "geracaoanuncios.v1.texto.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class GeraAnuncioTextoWorkerConfiguration {
    /** Cria o client HTTP da etapa para consumir pendências e callbacks do backend. */
    @Bean
    public GeraAnuncioTextoBackendClient geraAnuncioTextoBackendClient(WebClient.Builder builder, GeraAnuncioTextoWorkerProperties properties) {
        return new GeraAnuncioTextoBackendClient(builder, properties);
    }

    /** Cria o builder do payload operacional da etapa. */
    @Bean
    public GeraAnuncioTextoPromptBuilder geraAnuncioTextoPromptBuilder(ObjectMapper objectMapper) {
        return new GeraAnuncioTextoPromptBuilder(objectMapper);
    }

    /** Cria o validador da saída funcional da etapa. */
    @Bean
    public GeraAnuncioTextoResponseValidator geraAnuncioTextoResponseValidator() {
        return new GeraAnuncioTextoResponseValidator();
    }

    /** Cria o handler responsável por artefatos e métricas da etapa. */
    @Bean
    public GeraAnuncioTextoResponseHandler geraAnuncioTextoResponseHandler() {
        return new GeraAnuncioTextoResponseHandler();
    }

    /** Cria o processor específico da etapa. */
    @Bean
    public GeraAnuncioTextoProcessor geraAnuncioTextoProcessor(
            GeraAnuncioTextoPromptBuilder promptBuilder,
            GeraAnuncioTextoResponseValidator responseValidator,
            GeraAnuncioTextoResponseHandler responseHandler) {
        return new GeraAnuncioTextoProcessor(promptBuilder, responseValidator, responseHandler);
    }

    /** Compõe o worker genérico com client, processor e store de artefatos da etapa. */
    @Bean
    public PipelineWorker<GeraAnuncioTextoInput, GeraAnuncioTextoOutput> geraAnuncioTextoPipelineWorker(
            GeraAnuncioTextoBackendClient backendClient,
            GeraAnuncioTextoProcessor processor) {
        return new PipelineWorker<>(backendClient, processor, new InMemoryArtifactStore());
    }

    /** Cria o scheduler que aciona periodicamente o worker da etapa. */
    @Bean
    public GeraAnuncioTextoExecutionScheduler geraAnuncioTextoExecutionScheduler(
            @Qualifier("geraAnuncioTextoPipelineWorker") PipelineWorker<GeraAnuncioTextoInput, GeraAnuncioTextoOutput> worker,
            GeraAnuncioTextoWorkerProperties properties) {
        return new GeraAnuncioTextoExecutionScheduler(worker, properties);
    }
}
