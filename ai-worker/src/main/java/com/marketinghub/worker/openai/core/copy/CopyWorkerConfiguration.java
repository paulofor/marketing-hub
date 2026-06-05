package com.marketinghub.worker.openai.core.copy;

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

/** Responsabilidade: declarar os beans Spring necessários para executar a etapa copy no core OpenAI. */
@Configuration
@EnableConfigurationProperties({
        CopyWorkerProperties.class,
        OpenAiClientProperties.class
})
@ConditionalOnProperty(
        prefix = "copy.worker",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class CopyWorkerConfiguration {

    /** Cria o adapter HTTP da etapa copy para consultar e atualizar execuções no backend. */
    @Bean
    public CopyBackendClient copyBackendClient(
            WebClient.Builder webClientBuilder,
            CopyWorkerProperties properties,
            ObjectMapper objectMapper
    ) {
        return new CopyBackendClient(webClientBuilder, properties, objectMapper);
    }

    /** Cria o builder responsável por montar prompt, schema e request OpenAI da etapa copy. */
    @Bean
    public CopyPromptBuilder copyPromptBuilder(
            ObjectMapper objectMapper,
            CopyWorkerProperties copyProperties
    ) {
        return new CopyPromptBuilder(objectMapper, copyProperties);
    }

    /** Cria o validador da resposta JSON retornada pela OpenAI para a etapa copy. */
    @Bean
    public CopyResponseValidator copyResponseValidator(ObjectMapper objectMapper) {
        return new CopyResponseValidator(objectMapper);
    }

    /** Cria o handler de logs operacionais para sucesso ou falha da etapa copy. */
    @Bean
    public CopyResponseHandler copyResponseHandler() {
        return new CopyResponseHandler();
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

    /** Compõe o worker genérico com os ports específicos da etapa copy. */
    @Bean
    public StageWorker<CopyInput, CopyOutput> copyStageWorker(
            CopyBackendClient backendClient,
            CopyPromptBuilder promptBuilder,
            OpenAiClientPort openAiClient,
            CopyResponseValidator responseValidator,
            CopyResponseHandler responseHandler
    ) {
        return new StageWorker<>(
                backendClient,
                promptBuilder,
                openAiClient,
                responseValidator,
                responseHandler
        );
    }

    /** Cria o agendador periódico que aciona o worker da etapa copy. */
    @Bean
    public CopyExecutionScheduler copyExecutionScheduler(
            StageWorker<CopyInput, CopyOutput> worker,
            CopyWorkerProperties properties
    ) {
        return new CopyExecutionScheduler(worker, properties);
    }
}
