package com.marketinghub.worker.pipeline.hypothesispain;

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

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa Dor no pipeline genérico. */
@Configuration
@EnableConfigurationProperties({HypothesisPainWorkerProperties.class, OpenAiClientProperties.class})
@ConditionalOnProperty(prefix = "hypothesis-pain.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HypothesisPainWorkerConfiguration {
    /** Cria o adapter HTTP da etapa Dor para consultar e atualizar execuções no backend. */
    @Bean
    public HypothesisPainBackendClient hypothesisPainBackendClient(
            WebClient.Builder webClientBuilder,
            HypothesisPainWorkerProperties properties,
            ObjectMapper objectMapper) {
        return new HypothesisPainBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa Dor. */
    @Bean
    public HypothesisPainResponseValidator hypothesisPainResponseValidator(ObjectMapper objectMapper) {
        return new HypothesisPainResponseValidator(objectMapper);
    }

    /** Cria o cliente OpenAI compartilhado quando nenhuma outra etapa já o registrou. */
    @Bean
    @ConditionalOnMissingBean(OpenAiClientPort.class)
    public OpenAiClientPort hypothesisPainOpenAiClientPort(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            OpenAiClientProperties properties) {
        return new ResponsesApiOpenAiClient(webClientBuilder, objectMapper, properties);
    }

    /** Cria o ArtifactStore genérico usado para referenciar request, response e JSON normalizado da etapa. */
    @Bean
    @ConditionalOnMissingBean(ArtifactStore.class)
    public ArtifactStore artifactStore() {
        return new InMemoryArtifactStore();
    }

    /** Cria o processor específico responsável por construir a dor via OpenAI. */
    @Bean
    public HypothesisPainProcessor hypothesisPainProcessor(
            ObjectMapper objectMapper,
            HypothesisPainWorkerProperties properties,
            OpenAiClientPort openAiClient,
            HypothesisPainResponseValidator responseValidator,
            HypothesisPainBackendClient backendClient) {
        return new HypothesisPainProcessor(objectMapper, properties, openAiClient, responseValidator, backendClient);
    }

    /** Compõe o worker genérico com o port e processor específicos da etapa Dor. */
    @Bean
    public PipelineWorker<HypothesisPainInput, HypothesisPainOutput> hypothesisPainPipelineWorker(
            HypothesisPainBackendClient backendClient,
            HypothesisPainProcessor processor,
            ArtifactStore artifactStore) {
        return new PipelineWorker<>(backendClient, processor, artifactStore);
    }

    /** Cria o agendador periódico que aciona o worker da etapa Dor. */
    @Bean
    public HypothesisPainExecutionScheduler hypothesisPainExecutionScheduler(
            PipelineWorker<HypothesisPainInput, HypothesisPainOutput> worker,
            HypothesisPainWorkerProperties properties) {
        return new HypothesisPainExecutionScheduler(worker, properties);
    }
}
