package com.marketinghub.worker.pipeline.gerasalespagev1;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.openai.OpenAiClientProperties;
import com.marketinghub.worker.openai.core.openai.ResponsesApiOpenAiClient;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;
import com.marketinghub.worker.pipeline.ArtifactStore;
import com.marketinghub.worker.pipeline.InMemoryArtifactStore;
import com.marketinghub.worker.pipeline.PipelineWorker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: declarar beans Spring do executor GeraSalesPage v1. */
@Configuration
@EnableConfigurationProperties({GeraSalesPageWorkerProperties.class, OpenAiClientProperties.class})
@ConditionalOnProperty(prefix = "gerasalespage.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class GeraSalesPageWorkerConfiguration {
    /** Cria o adapter HTTP do GeraSalesPage v1 para consultar e atualizar execuções no backend. */
    @Bean
    public GeraSalesPageBackendClient geraSalesPageBackendClient(
            WebClient.Builder webClientBuilder,
            GeraSalesPageWorkerProperties properties,
            ObjectMapper objectMapper) {
        return new GeraSalesPageBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o validador de resposta JSON para todas as etapas do GeraSalesPage v1. */
    @Bean
    public GeraSalesPageResponseValidator geraSalesPageResponseValidator(ObjectMapper objectMapper) {
        return new GeraSalesPageResponseValidator(objectMapper);
    }

    /** Cria o cliente OpenAI compartilhado quando outro worker ainda não registrou um. */
    @Bean
    @ConditionalOnMissingBean(OpenAiClientPort.class)
    public OpenAiClientPort geraSalesPageOpenAiClientPort(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            OpenAiClientProperties properties) {
        return new ResponsesApiOpenAiClient(webClientBuilder, objectMapper, properties);
    }

    /** Cria o ArtifactStore genérico para auditoria local de request e response. */
    @Bean
    @ConditionalOnMissingBean(ArtifactStore.class)
    public ArtifactStore artifactStore() {
        return new InMemoryArtifactStore();
    }

    /** Cria o processor que executa etapas do GeraSalesPage v1 com templates vindos do banco. */
    @Bean
    public GeraSalesPageProcessor geraSalesPageProcessor(
            ObjectMapper objectMapper,
            OpenAiClientPort openAiClient,
            GeraSalesPageResponseValidator responseValidator,
            GeraSalesPageBackendClient backendClient,
            GeraSalesPageWorkerProperties properties) {
        return new GeraSalesPageProcessor(objectMapper, openAiClient, responseValidator, backendClient, properties.serviceTier());
    }

    /** Compõe o worker genérico com o adapter e processor do GeraSalesPage v1. */
    @Bean
    public PipelineWorker<GeraSalesPageInput, GeraSalesPageOutput> geraSalesPagePipelineWorker(
            GeraSalesPageBackendClient backendClient,
            GeraSalesPageProcessor processor,
            ArtifactStore artifactStore) {
        return new PipelineWorker<>(backendClient, processor, artifactStore);
    }

    /** Cria o scheduler periódico do GeraSalesPage v1. */
    @Bean
    public GeraSalesPageExecutionScheduler geraSalesPageExecutionScheduler(
            PipelineWorker<GeraSalesPageInput, GeraSalesPageOutput> worker,
            GeraSalesPageWorkerProperties properties) {
        return new GeraSalesPageExecutionScheduler(worker, properties);
    }
}
