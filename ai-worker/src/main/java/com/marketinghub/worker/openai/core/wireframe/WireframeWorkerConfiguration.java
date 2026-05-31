package com.marketinghub.worker.openai.core.wireframe;

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

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa wireframe no core OpenAI. */
@Configuration
@EnableConfigurationProperties({
        WireframeWorkerProperties.class,
        OpenAiClientProperties.class
})
@ConditionalOnProperty(
        prefix = "wireframe.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class WireframeWorkerConfiguration {

    /** Cria o adapter HTTP da etapa wireframe para consultar e atualizar execuções no backend. */
    @Bean
    public WireframeBackendClient wireframeBackendClient(
            WebClient.Builder webClientBuilder,
            WireframeWorkerProperties properties,
            ObjectMapper objectMapper
    ) {
        return new WireframeBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o builder responsável por montar prompt, schema e request OpenAI da etapa wireframe. */
    @Bean
    public WireframePromptBuilder wireframePromptBuilder(
            ObjectMapper objectMapper,
            OpenAiClientProperties openAiProperties,
            WireframeWorkerProperties wireframeProperties
    ) {
        return new WireframePromptBuilder(objectMapper, openAiProperties, wireframeProperties);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa wireframe. */
    @Bean
    public WireframeResponseValidator wireframeResponseValidator(ObjectMapper objectMapper) {
        return new WireframeResponseValidator(objectMapper);
    }

    /** Cria o handler de logs operacionais para sucesso ou falha da etapa wireframe. */
    @Bean
    public WireframeResponseHandler wireframeResponseHandler() {
        return new WireframeResponseHandler();
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

    /** Compõe o worker genérico com os ports específicos da etapa wireframe. */
    @Bean
    public StageWorker<WireframeInput, WireframeOutput> wireframeStageWorker(
            WireframeBackendClient backendClient,
            WireframePromptBuilder promptBuilder,
            OpenAiClientPort openAiClient,
            WireframeResponseValidator responseValidator,
            WireframeResponseHandler responseHandler
    ) {
        return new StageWorker<>(
                backendClient,
                promptBuilder,
                openAiClient,
                responseValidator,
                responseHandler
        );
    }

    /** Cria o agendador periódico que aciona o worker da etapa wireframe. */
    @Bean
    public WireframeExecutionScheduler wireframeExecutionScheduler(
            StageWorker<WireframeInput, WireframeOutput> worker,
            WireframeWorkerProperties properties
    ) {
        return new WireframeExecutionScheduler(worker, properties);
    }
}
