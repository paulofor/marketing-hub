package com.marketinghub.worker.pipeline.hypothesismechanism;

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

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa Mecanismo no pipeline genérico. */
@Configuration
@EnableConfigurationProperties({HypothesisMechanismWorkerProperties.class, OpenAiClientProperties.class})
@ConditionalOnProperty(prefix = "hypothesis-mechanism.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HypothesisMechanismWorkerConfiguration {
    /** Cria o adapter HTTP da etapa Mecanismo para consultar e atualizar execuções no backend. */
    @Bean
    public HypothesisMechanismBackendClient hypothesisMechanismBackendClient(
            WebClient.Builder webClientBuilder,
            HypothesisMechanismWorkerProperties properties,
            ObjectMapper objectMapper) {
        return new HypothesisMechanismBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa Mecanismo. */
    @Bean
    public HypothesisMechanismResponseValidator hypothesisMechanismResponseValidator(ObjectMapper objectMapper) {
        return new HypothesisMechanismResponseValidator(objectMapper);
    }

    /** Cria o cliente OpenAI compartilhado quando nenhuma outra etapa já o registrou. */
    @Bean
    @ConditionalOnMissingBean(OpenAiClientPort.class)
    public OpenAiClientPort hypothesisMechanismOpenAiClientPort(
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

    /** Cria o processor específico responsável por construir o mecanismo via OpenAI. */
    @Bean
    public HypothesisMechanismProcessor hypothesisMechanismProcessor(
            ObjectMapper objectMapper,
            HypothesisMechanismWorkerProperties properties,
            OpenAiClientPort openAiClient,
            HypothesisMechanismResponseValidator responseValidator,
            HypothesisMechanismBackendClient backendClient) {
        return new HypothesisMechanismProcessor(objectMapper, properties, openAiClient, responseValidator, backendClient);
    }

    /** Compõe o worker genérico com o port e processor específicos da etapa Mecanismo. */
    @Bean
    public PipelineWorker<HypothesisMechanismInput, HypothesisMechanismOutput> hypothesisMechanismPipelineWorker(
            HypothesisMechanismBackendClient backendClient,
            HypothesisMechanismProcessor processor,
            ArtifactStore artifactStore) {
        return new PipelineWorker<>(backendClient, processor, artifactStore);
    }

    /** Cria o agendador periódico que aciona o worker da etapa Mecanismo. */
    @Bean
    public HypothesisMechanismExecutionScheduler hypothesisMechanismExecutionScheduler(
            PipelineWorker<HypothesisMechanismInput, HypothesisMechanismOutput> worker,
            HypothesisMechanismWorkerProperties properties) {
        return new HypothesisMechanismExecutionScheduler(worker, properties);
    }
}
