package com.marketinghub.feo.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configura os clientes HTTP usados pela FEO para backend e integrações externas.
 */
@Configuration
public class WebClientConfiguration {

    private static final int OPENAI_MAX_MEMORY_BYTES = 16 * 1024 * 1024;

    /**
     * Cria WebClient apontando para a API do backend principal.
     */
    @Bean
    WebClient feoBackendWebClient(FeoProperties properties) {
        return WebClient.builder().baseUrl(properties.backendBaseUrl()).build();
    }

    /**
     * Cria WebClient para geração de imagens na OpenAI.
     */
    @Bean
    @Qualifier("openAiWebClient")
    WebClient openAiWebClient(FeoProperties properties) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(OPENAI_MAX_MEMORY_BYTES))
                .build();
        return WebClient.builder()
                .baseUrl(properties.openaiBaseUrl())
                .exchangeStrategies(strategies)
                .build();
    }
}
