package com.marketinghub.feo.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configura o cliente HTTP usado exclusivamente para conversar com o backend.
 */
@Configuration
public class WebClientConfiguration {

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
        return WebClient.builder().baseUrl(properties.openaiBaseUrl()).build();
    }
}
