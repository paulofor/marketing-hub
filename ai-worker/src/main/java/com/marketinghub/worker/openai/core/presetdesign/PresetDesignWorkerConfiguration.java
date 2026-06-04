package com.marketinghub.worker.openai.core.presetdesign;

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

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa presetdesign no core OpenAI. */
@Configuration
@EnableConfigurationProperties({
        PresetDesignWorkerProperties.class,
        OpenAiClientProperties.class
})
@ConditionalOnProperty(
        prefix = "presetdesign.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class PresetDesignWorkerConfiguration {

    /** Cria o adapter HTTP da etapa presetdesign para consultar e atualizar execuções no backend. */
    @Bean
    public PresetDesignBackendClient presetdesignBackendClient(
            WebClient.Builder webClientBuilder,
            PresetDesignWorkerProperties properties,
            ObjectMapper objectMapper
    ) {
        return new PresetDesignBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o builder responsável por montar prompt, schema e request OpenAI da etapa presetdesign. */
    @Bean
    public PresetDesignPromptBuilder presetdesignPromptBuilder(
            ObjectMapper objectMapper,
            PresetDesignWorkerProperties presetdesignProperties
    ) {
        return new PresetDesignPromptBuilder(objectMapper, presetdesignProperties);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa presetdesign. */
    @Bean
    public PresetDesignResponseValidator presetdesignResponseValidator(ObjectMapper objectMapper) {
        return new PresetDesignResponseValidator(objectMapper);
    }

    /** Cria o handler de logs operacionais para sucesso ou falha da etapa presetdesign. */
    @Bean
    public PresetDesignResponseHandler presetdesignResponseHandler() {
        return new PresetDesignResponseHandler();
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

    /** Compõe o worker genérico com os ports específicos da etapa presetdesign. */
    @Bean
    public StageWorker<PresetDesignInput, PresetDesignOutput> presetdesignStageWorker(
            PresetDesignBackendClient backendClient,
            PresetDesignPromptBuilder promptBuilder,
            OpenAiClientPort openAiClient,
            PresetDesignResponseValidator responseValidator,
            PresetDesignResponseHandler responseHandler
    ) {
        return new StageWorker<>(
                backendClient,
                promptBuilder,
                openAiClient,
                responseValidator,
                responseHandler
        );
    }

    /** Cria o agendador periódico que aciona o worker da etapa presetdesign. */
    @Bean
    public PresetDesignExecutionScheduler presetdesignExecutionScheduler(
            StageWorker<PresetDesignInput, PresetDesignOutput> worker,
            PresetDesignWorkerProperties properties
    ) {
        return new PresetDesignExecutionScheduler(worker, properties);
    }
}
