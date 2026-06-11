package com.marketinghub.worker.pipeline.hypothesisoffer;

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

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa Oferta no pipeline genérico. */
@Configuration
@EnableConfigurationProperties({HypothesisOfferWorkerProperties.class, OpenAiClientProperties.class})
@ConditionalOnProperty(prefix = "hypothesis-offer.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class HypothesisOfferWorkerConfiguration {
    /** Cria o adapter HTTP da etapa Oferta para consultar e atualizar execuções no backend. */
    @Bean
    public HypothesisOfferBackendClient hypothesisOfferBackendClient(
            WebClient.Builder webClientBuilder,
            HypothesisOfferWorkerProperties properties,
            ObjectMapper objectMapper) {
        return new HypothesisOfferBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa Oferta. */
    @Bean
    public HypothesisOfferResponseValidator hypothesisOfferResponseValidator(ObjectMapper objectMapper) {
        return new HypothesisOfferResponseValidator(objectMapper);
    }

    /** Cria o cliente OpenAI compartilhado quando nenhuma outra etapa já o registrou. */
    @Bean
    @ConditionalOnMissingBean(OpenAiClientPort.class)
    public OpenAiClientPort hypothesisOfferOpenAiClientPort(
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

    /** Cria o processor específico responsável por construir a oferta via OpenAI. */
    @Bean
    public HypothesisOfferProcessor hypothesisOfferProcessor(
            ObjectMapper objectMapper,
            HypothesisOfferWorkerProperties properties,
            OpenAiClientPort openAiClient,
            HypothesisOfferResponseValidator responseValidator,
            HypothesisOfferBackendClient backendClient) {
        return new HypothesisOfferProcessor(objectMapper, properties, openAiClient, responseValidator, backendClient);
    }

    /** Compõe o worker genérico com o port e processor específicos da etapa Oferta. */
    @Bean
    public PipelineWorker<HypothesisOfferInput, HypothesisOfferOutput> hypothesisOfferPipelineWorker(
            HypothesisOfferBackendClient backendClient,
            HypothesisOfferProcessor processor,
            ArtifactStore artifactStore) {
        return new PipelineWorker<>(backendClient, processor, artifactStore);
    }

    /** Cria o agendador periódico que aciona o worker da etapa Oferta. */
    @Bean
    public HypothesisOfferExecutionScheduler hypothesisOfferExecutionScheduler(
            PipelineWorker<HypothesisOfferInput, HypothesisOfferOutput> worker,
            HypothesisOfferWorkerProperties properties) {
        return new HypothesisOfferExecutionScheduler(worker, properties);
    }
}
