package com.marketinghub.worker.openai.core.imageplanning;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.openai.OpenAiClientProperties;
import com.marketinghub.worker.openai.core.openai.ResponsesApiOpenAiClient;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa imageplanning no core OpenAI. */
@Configuration
@EnableConfigurationProperties({
        ImagePlanningWorkerProperties.class,
        OpenAiClientProperties.class
})
@ConditionalOnProperty(
        prefix = "imageplanning.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ImagePlanningWorkerConfiguration {

    /** Cria o adapter HTTP da etapa imageplanning para consultar e atualizar execuções no backend. */
    @Bean
    public ImagePlanningBackendClient imageplanningBackendClient(
            WebClient.Builder webClientBuilder,
            ImagePlanningWorkerProperties properties,
            ObjectMapper objectMapper
    ) {
        return new ImagePlanningBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o builder responsável por montar prompt, schema e request OpenAI da etapa imageplanning. */
    @Bean
    public ImagePlanningPromptBuilder imageplanningPromptBuilder(
            ObjectMapper objectMapper,
            ImagePlanningWorkerProperties imagePlanningProperties
    ) {
        return new ImagePlanningPromptBuilder(objectMapper, imagePlanningProperties);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa imageplanning. */
    @Bean
    public ImagePlanningResponseValidator imageplanningResponseValidator(ObjectMapper objectMapper) {
        return new ImagePlanningResponseValidator(objectMapper);
    }

    /** Cria o handler de logs operacionais para sucesso ou falha da etapa imageplanning. */
    @Bean
    public ImagePlanningResponseHandler imageplanningResponseHandler() {
        return new ImagePlanningResponseHandler();
    }

    /** Cria o cliente OpenAI compartilhado quando nenhuma outra etapa do core já o registrou. */
    @Bean
    @ConditionalOnMissingBean(OpenAiClientPort.class)
    public OpenAiClientPort openAiClientPort(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            OpenAiClientProperties properties
    ) {
        return new ResponsesApiOpenAiClient(webClientBuilder, objectMapper, properties);
    }

    /** Compõe o worker genérico com os ports específicos da etapa imageplanning. */
    @Bean
    public StageWorker<ImagePlanningInput, ImagePlanningOutput> imageplanningStageWorker(
            ImagePlanningBackendClient backendClient,
            ImagePlanningPromptBuilder promptBuilder,
            OpenAiClientPort openAiClient,
            ImagePlanningResponseValidator responseValidator,
            ImagePlanningResponseHandler responseHandler
    ) {
        return new StageWorker<>(
                backendClient,
                promptBuilder,
                openAiClient,
                responseValidator,
                responseHandler
        );
    }

    /** Cria o agendador periódico que aciona o worker da etapa imageplanning. */
    @Bean
    public ImagePlanningExecutionScheduler imageplanningExecutionScheduler(
            StageWorker<ImagePlanningInput, ImagePlanningOutput> worker,
            ImagePlanningWorkerProperties properties
    ) {
        return new ImagePlanningExecutionScheduler(worker, properties);
    }
}
