package com.marketinghub.worker.pipeline.hypothesisresult;

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

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa Resultado no pipeline genérico. */
@Configuration
@EnableConfigurationProperties({HypothesisResultWorkerProperties.class, OpenAiClientProperties.class})
@ConditionalOnProperty(prefix = "hypothesis-result.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HypothesisResultWorkerConfiguration {
    /** Cria o adapter HTTP da etapa Resultado para consultar e atualizar execuções no backend. */
    @Bean
    public HypothesisResultBackendClient hypothesisResultBackendClient(
            WebClient.Builder webClientBuilder,
            HypothesisResultWorkerProperties properties,
            ObjectMapper objectMapper) {
        return new HypothesisResultBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa Resultado. */
    @Bean
    public HypothesisResultResponseValidator hypothesisResultResponseValidator(ObjectMapper objectMapper) {
        return new HypothesisResultResponseValidator(objectMapper);
    }

    /** Cria o cliente OpenAI compartilhado quando nenhuma outra etapa já o registrou. */
    @Bean
    @ConditionalOnMissingBean(OpenAiClientPort.class)
    public OpenAiClientPort hypothesisResultOpenAiClientPort(
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

    /** Cria o processor específico responsável por construir o resultado via OpenAI. */
    @Bean
    public HypothesisResultProcessor hypothesisResultProcessor(
            ObjectMapper objectMapper,
            HypothesisResultWorkerProperties properties,
            OpenAiClientPort openAiClient,
            HypothesisResultResponseValidator responseValidator,
            HypothesisResultBackendClient backendClient) {
        return new HypothesisResultProcessor(objectMapper, properties, openAiClient, responseValidator, backendClient);
    }

    /** Compõe o worker genérico com o port e processor específicos da etapa Resultado. */
    @Bean
    public PipelineWorker<HypothesisResultInput, HypothesisResultOutput> hypothesisResultPipelineWorker(
            HypothesisResultBackendClient backendClient,
            HypothesisResultProcessor processor,
            ArtifactStore artifactStore) {
        return new PipelineWorker<>(backendClient, processor, artifactStore);
    }

    /** Cria o agendador periódico que aciona o worker da etapa Resultado. */
    @Bean
    public HypothesisResultExecutionScheduler hypothesisResultExecutionScheduler(
            PipelineWorker<HypothesisResultInput, HypothesisResultOutput> worker,
            HypothesisResultWorkerProperties properties) {
        return new HypothesisResultExecutionScheduler(worker, properties);
    }
}
