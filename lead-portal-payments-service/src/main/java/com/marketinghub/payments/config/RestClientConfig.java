package com.marketinghub.payments.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient mercadoPagoRestClient(MercadoPagoProperties properties) {
        if (properties.getAccessToken() == null || properties.getAccessToken().isBlank()) {
            throw new IllegalStateException("MERCADO_PAGO_ACCESS_TOKEN não configurado");
        }
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}
