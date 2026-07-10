package com.marketinghub.worker.openai.core.imagegeneration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.creative.CreativeImageOptimizer;
import com.marketinghub.worker.frameworkimage.FrameworkImageStorageClient;
import com.marketinghub.worker.openai.core.StageWorker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa imagegeneration no core OpenAI. */
@Configuration
@EnableConfigurationProperties(ImageGenerationWorkerProperties.class)
@ConditionalOnProperty(
        prefix = "imagegeneration.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class ImageGenerationWorkerConfiguration {

    /** Cria o adapter HTTP/storage da etapa imagegeneration para consultar, atualizar e publicar imagens. */
    @Bean
    public ImageGenerationBackendClient imageGenerationBackendClient(
            FrameworkImageStorageClient storageClient,
            CreativeImageOptimizer imageOptimizer,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            ImageGenerationWorkerProperties properties,
            @Value("${openai.max-in-memory-size-bytes:52428800}") int maxInMemorySizeBytes
    ) {
        return new ImageGenerationBackendClient(
                storageClient,
                imageOptimizer,
                webClientBuilder,
                objectMapper,
                properties,
                maxInMemorySizeBytes
        );
    }

    /** Cria o builder responsável por montar o request de imagem da OpenAI para a etapa imagegeneration. */
    @Bean
    public ImageGenerationPromptBuilder imageGenerationPromptBuilder(
            ObjectMapper objectMapper,
            ImageGenerationWorkerProperties properties
    ) {
        return new ImageGenerationPromptBuilder(objectMapper, properties);
    }

    /** Cria o validador da resposta de imagem retornada pela OpenAI para a etapa imagegeneration. */
    @Bean
    public ImageGenerationResponseValidator imageGenerationResponseValidator(ObjectMapper objectMapper) {
        return new ImageGenerationResponseValidator(objectMapper);
    }

    /** Cria o handler de logs operacionais para sucesso ou falha da etapa imagegeneration. */
    @Bean
    public ImageGenerationResponseHandler imageGenerationResponseHandler() {
        return new ImageGenerationResponseHandler();
    }

    /** Compõe o worker genérico com ports específicos de imagem sem interferir no cliente textual das demais etapas. */
    @Bean
    public StageWorker<ImageGenerationInput, ImageGenerationOutput> imageGenerationStageWorker(
            ImageGenerationBackendClient backendClient,
            ImageGenerationPromptBuilder promptBuilder,
            ImageGenerationResponseValidator responseValidator,
            ImageGenerationResponseHandler responseHandler,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            ImageGenerationWorkerProperties properties,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.allow-local-base-url:false}") boolean allowLocalBaseUrl,
            @Value("${openai.max-in-memory-size-bytes:52428800}") int maxInMemorySizeBytes
    ) {
        ImageGenerationOpenAiClient openAiClient = new ImageGenerationOpenAiClient(
                webClientBuilder,
                objectMapper,
                properties,
                apiKey,
                baseUrl,
                allowLocalBaseUrl,
                maxInMemorySizeBytes
        );
        return new StageWorker<>(backendClient, promptBuilder, openAiClient, responseValidator, responseHandler);
    }

    /** Cria o agendador periódico que aciona o worker da etapa imagegeneration. */
    @Bean
    public ImageGenerationExecutionScheduler imageGenerationExecutionScheduler(
            StageWorker<ImageGenerationInput, ImageGenerationOutput> worker,
            ImageGenerationWorkerProperties properties
    ) {
        return new ImageGenerationExecutionScheduler(worker, properties);
    }
}
