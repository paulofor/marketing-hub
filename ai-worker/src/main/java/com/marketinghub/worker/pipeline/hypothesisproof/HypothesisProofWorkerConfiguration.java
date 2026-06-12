package com.marketinghub.worker.pipeline.hypothesisproof;

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

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa Prova no pipeline genérico. */
@Configuration
@EnableConfigurationProperties({HypothesisProofWorkerProperties.class, OpenAiClientProperties.class})
@ConditionalOnProperty(prefix = "hypothesis-proof.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HypothesisProofWorkerConfiguration {
    /** Cria o adapter HTTP da etapa Prova para consultar e atualizar execuções no backend. */
    @Bean
    public HypothesisProofBackendClient hypothesisProofBackendClient(
            WebClient.Builder webClientBuilder,
            HypothesisProofWorkerProperties properties,
            ObjectMapper objectMapper) {
        return new HypothesisProofBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa Prova. */
    @Bean
    public HypothesisProofResponseValidator hypothesisProofResponseValidator(ObjectMapper objectMapper) {
        return new HypothesisProofResponseValidator(objectMapper);
    }

    /** Cria o cliente OpenAI compartilhado quando nenhuma outra etapa já o registrou. */
    @Bean
    @ConditionalOnMissingBean(OpenAiClientPort.class)
    public OpenAiClientPort hypothesisProofOpenAiClientPort(
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

    /** Cria o processor específico responsável por construir a prova via OpenAI. */
    @Bean
    public HypothesisProofProcessor hypothesisProofProcessor(
            ObjectMapper objectMapper,
            HypothesisProofWorkerProperties properties,
            OpenAiClientPort openAiClient,
            HypothesisProofResponseValidator responseValidator,
            HypothesisProofBackendClient backendClient) {
        return new HypothesisProofProcessor(objectMapper, properties, openAiClient, responseValidator, backendClient);
    }

    /** Compõe o worker genérico com o port e processor específicos da etapa Prova. */
    @Bean
    public PipelineWorker<HypothesisProofInput, HypothesisProofOutput> hypothesisProofPipelineWorker(
            HypothesisProofBackendClient backendClient,
            HypothesisProofProcessor processor,
            ArtifactStore artifactStore) {
        return new PipelineWorker<>(backendClient, processor, artifactStore);
    }

    /** Cria o agendador periódico que aciona o worker da etapa Prova. */
    @Bean
    public HypothesisProofExecutionScheduler hypothesisProofExecutionScheduler(
            PipelineWorker<HypothesisProofInput, HypothesisProofOutput> worker,
            HypothesisProofWorkerProperties properties) {
        return new HypothesisProofExecutionScheduler(worker, properties);
    }
}
