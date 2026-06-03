package com.marketinghub.worker.openai.core.qualityreview;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.frameworkimage.FrameworkImageStorageClient;
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

/** Responsabilidade: declarar os beans Spring necessários para executar a revisão visual no core OpenAI. */
@Configuration
@EnableConfigurationProperties({QualityReviewWorkerProperties.class, OpenAiClientProperties.class})
@ConditionalOnProperty(prefix = "qualityreview.worker", name = "enabled", havingValue = "true", matchIfMissing = false)
public class QualityReviewWorkerConfiguration {

    /** Cria o adapter HTTP da revisão visual para consultar e atualizar execuções no backend. */
    @Bean
    public QualityReviewBackendClient qualityReviewBackendClient(WebClient.Builder webClientBuilder, QualityReviewWorkerProperties properties, ObjectMapper objectMapper) {
        return new QualityReviewBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o serviço que renderiza o HTML final em Chromium e publica screenshots públicos. */
    @Bean
    public QualityReviewScreenshotService qualityReviewScreenshotService(FrameworkImageStorageClient storageClient, QualityReviewWorkerProperties properties) {
        return new PlaywrightQualityReviewScreenshotService(storageClient, properties);
    }

    /** Cria o builder responsável por montar prompt, schema e request OpenAI multimodal da revisão visual. */
    @Bean
    public QualityReviewPromptBuilder qualityReviewPromptBuilder(
            ObjectMapper objectMapper,
            OpenAiClientProperties openAiProperties,
            QualityReviewWorkerProperties properties,
            QualityReviewScreenshotService screenshotService) {
        return new QualityReviewPromptBuilder(objectMapper, openAiProperties, properties, screenshotService);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a revisão visual. */
    @Bean
    public QualityReviewResponseValidator qualityReviewResponseValidator(ObjectMapper objectMapper) {
        return new QualityReviewResponseValidator(objectMapper);
    }

    /** Cria o handler de logs operacionais para sucesso ou falha da revisão visual. */
    @Bean
    public QualityReviewResponseHandler qualityReviewResponseHandler() {
        return new QualityReviewResponseHandler();
    }

    /** Cria o cliente OpenAI compartilhado quando nenhuma outra etapa do core já o registrou. */
    @Bean
    @ConditionalOnMissingBean(OpenAiClientPort.class)
    public OpenAiClientPort openAiClientPort(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, OpenAiClientProperties properties) {
        return new ResponsesApiOpenAiClient(webClientBuilder, objectMapper, properties);
    }

    /** Compõe o worker genérico com os ports específicos da revisão visual. */
    @Bean
    public StageWorker<QualityReviewInput, QualityReviewOutput> qualityReviewStageWorker(
            QualityReviewBackendClient backendClient,
            QualityReviewPromptBuilder promptBuilder,
            OpenAiClientPort openAiClient,
            QualityReviewResponseValidator responseValidator,
            QualityReviewResponseHandler responseHandler) {
        return new StageWorker<>(backendClient, promptBuilder, openAiClient, responseValidator, responseHandler);
    }

    /** Cria o agendador periódico que aciona o worker da revisão visual. */
    @Bean
    public QualityReviewExecutionScheduler qualityReviewExecutionScheduler(StageWorker<QualityReviewInput, QualityReviewOutput> worker, QualityReviewWorkerProperties properties) {
        return new QualityReviewExecutionScheduler(worker, properties);
    }
}
