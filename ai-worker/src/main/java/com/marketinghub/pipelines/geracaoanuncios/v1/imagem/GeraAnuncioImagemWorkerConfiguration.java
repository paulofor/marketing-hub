package com.marketinghub.pipelines.geracaoanuncios.v1.imagem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.pipeline.InMemoryArtifactStore;
import com.marketinghub.worker.pipeline.PipelineWorker;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa Imagem do GeracaoAnuncios v1. */
@Configuration
@EnableConfigurationProperties(GeraAnuncioImagemWorkerProperties.class)
@ConditionalOnProperty(prefix = "geracaoanuncios.v1.imagem.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class GeraAnuncioImagemWorkerConfiguration {
    /** Cria o client HTTP da etapa para consumir pendências e callbacks do backend. */
    @Bean
    public GeraAnuncioImagemBackendClient geraAnuncioImagemBackendClient(WebClient.Builder builder, GeraAnuncioImagemWorkerProperties properties) {
        return new GeraAnuncioImagemBackendClient(builder, properties);
    }

    /** Cria o builder do payload operacional da etapa. */
    @Bean
    public GeraAnuncioImagemPromptBuilder geraAnuncioImagemPromptBuilder(ObjectMapper objectMapper) {
        return new GeraAnuncioImagemPromptBuilder(objectMapper);
    }

    /** Cria o validador da saída funcional da etapa. */
    @Bean
    public GeraAnuncioImagemResponseValidator geraAnuncioImagemResponseValidator() {
        return new GeraAnuncioImagemResponseValidator();
    }

    /** Cria o handler responsável por artefatos e métricas da etapa. */
    @Bean
    public GeraAnuncioImagemResponseHandler geraAnuncioImagemResponseHandler() {
        return new GeraAnuncioImagemResponseHandler();
    }

    /** Cria o processor específico da etapa. */
    @Bean
    public GeraAnuncioImagemProcessor geraAnuncioImagemProcessor(
            GeraAnuncioImagemPromptBuilder promptBuilder,
            GeraAnuncioImagemResponseValidator responseValidator,
            GeraAnuncioImagemResponseHandler responseHandler) {
        return new GeraAnuncioImagemProcessor(promptBuilder, responseValidator, responseHandler);
    }

    /** Compõe o worker genérico com client, processor e store de artefatos da etapa. */
    @Bean
    public PipelineWorker<GeraAnuncioImagemInput, GeraAnuncioImagemOutput> geraAnuncioImagemPipelineWorker(
            GeraAnuncioImagemBackendClient backendClient,
            GeraAnuncioImagemProcessor processor) {
        return new PipelineWorker<>(backendClient, processor, new InMemoryArtifactStore());
    }

    /** Cria o scheduler que aciona periodicamente o worker da etapa. */
    @Bean
    public GeraAnuncioImagemExecutionScheduler geraAnuncioImagemExecutionScheduler(
            @Qualifier("geraAnuncioImagemPipelineWorker") PipelineWorker<GeraAnuncioImagemInput, GeraAnuncioImagemOutput> worker,
            GeraAnuncioImagemWorkerProperties properties) {
        return new GeraAnuncioImagemExecutionScheduler(worker, properties);
    }
}
