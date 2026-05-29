package com.marketinghub.worker.openai.core.wireframe;

import com.marketinghub.worker.openai.core.OpenAiWorkerProperties;
import com.marketinghub.worker.openai.core.StageWorker;
import com.marketinghub.worker.openai.core.port.OpenAiClientPort;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WireframeWorkerConfiguration {

    @Bean
    public OpenAiWorkerProperties wireframeWorkerProperties(
            @Value("${wireframe.worker.enabled:true}") boolean enabled,
            @Value("${wireframe.worker.pending-limit:10}") int pendingLimit,
            @Value("${openai.timeout:PT30M}") Duration timeout
    ) {
        return new OpenAiWorkerProperties(enabled, pendingLimit, timeout);
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
}
