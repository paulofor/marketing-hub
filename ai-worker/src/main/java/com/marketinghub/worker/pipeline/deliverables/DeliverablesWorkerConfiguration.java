package com.marketinghub.worker.pipeline.deliverables;

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

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa deliverables no pipeline genérico. */
@Configuration
@EnableConfigurationProperties({DeliverablesWorkerProperties.class, OpenAiClientProperties.class})
@ConditionalOnProperty(prefix = "deliverables.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class DeliverablesWorkerConfiguration {
    /** Cria o adapter HTTP da etapa deliverables para consultar e atualizar execuções no backend. */
    @Bean
    public DeliverablesBackendClient deliverablesBackendClient(WebClient.Builder webClientBuilder, DeliverablesWorkerProperties properties, ObjectMapper objectMapper) {
        return new DeliverablesBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa deliverables. */
    @Bean
    public DeliverablesResponseValidator deliverablesResponseValidator(ObjectMapper objectMapper) {
        return new DeliverablesResponseValidator(objectMapper);
    }

    /** Cria o cliente OpenAI compartilhado quando nenhuma outra etapa já o registrou. */
    @Bean
    @ConditionalOnMissingBean(OpenAiClientPort.class)
    public OpenAiClientPort pipelineOpenAiClientPort(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, OpenAiClientProperties properties) {
        return new ResponsesApiOpenAiClient(webClientBuilder, objectMapper, properties);
    }

    /** Cria o ArtifactStore genérico usado para referenciar request, response e JSON normalizado da etapa. */
    @Bean
    @ConditionalOnMissingBean(ArtifactStore.class)
    public ArtifactStore artifactStore() {
        return new InMemoryArtifactStore();
    }

    /** Cria o processor específico responsável por gerar os entregáveis via OpenAI. */
    @Bean
    public DeliverablesProcessor deliverablesProcessor(
            ObjectMapper objectMapper,
            DeliverablesWorkerProperties properties,
            OpenAiClientPort openAiClient,
            DeliverablesResponseValidator responseValidator,
            DeliverablesBackendClient backendClient) {
        return new DeliverablesProcessor(objectMapper, properties, openAiClient, responseValidator, backendClient);
    }

    /** Compõe o worker genérico com o port e processor específicos da etapa deliverables. */
    @Bean
    public PipelineWorker<DeliverablesInput, DeliverablesOutput> deliverablesPipelineWorker(
            DeliverablesBackendClient backendClient,
            DeliverablesProcessor processor,
            ArtifactStore artifactStore) {
        return new PipelineWorker<>(backendClient, processor, artifactStore);
    }

    /** Cria o agendador periódico que aciona o worker da etapa deliverables. */
    @Bean
    public DeliverablesExecutionScheduler deliverablesExecutionScheduler(
            PipelineWorker<DeliverablesInput, DeliverablesOutput> worker,
            DeliverablesWorkerProperties properties) {
        return new DeliverablesExecutionScheduler(worker, properties);
    }
}
