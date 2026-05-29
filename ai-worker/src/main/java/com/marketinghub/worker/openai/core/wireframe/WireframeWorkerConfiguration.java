package com.marketinghub.worker.openai.core.wireframe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.openai.OpenAiClientProperties;
import com.marketinghub.worker.openai.core.openai.ResponsesApiOpenAiClient;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

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

    @Bean
    public WireframeBackendClient wireframeBackendClient(
            WebClient.Builder webClientBuilder,
            WireframeWorkerProperties properties,
            ObjectMapper objectMapper
    ) {
        return new WireframeBackendClient(webClientBuilder, properties, objectMapper);
    }

    @Bean
    public WireframePromptBuilder wireframePromptBuilder(
            ObjectMapper objectMapper,
            OpenAiClientProperties openAiProperties,
            WireframeWorkerProperties wireframeProperties
    ) {
        return new WireframePromptBuilder(objectMapper, openAiProperties, wireframeProperties);
    }

    @Bean
    public WireframeResponseValidator wireframeResponseValidator(ObjectMapper objectMapper) {
        return new WireframeResponseValidator(objectMapper);
    }

    @Bean
    public WireframeResponseHandler wireframeResponseHandler() {
        return new WireframeResponseHandler();
    }

    @Bean
    public OpenAiClientPort openAiClientPort(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            OpenAiClientProperties properties
    ) {
        return new ResponsesApiOpenAiClient(webClientBuilder, objectMapper, properties);
    }

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

    @Bean
    public WireframeExecutionScheduler wireframeExecutionScheduler(
            StageWorker<WireframeInput, WireframeOutput> worker,
            WireframeWorkerProperties properties
    ) {
        return new WireframeExecutionScheduler(worker, properties);
    }
}
