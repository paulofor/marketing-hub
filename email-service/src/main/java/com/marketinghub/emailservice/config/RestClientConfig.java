package com.marketinghub.emailservice.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient marketingHubRestClient(MarketingHubClientProperties properties) {
        return RestClient.builder()
                .requestFactory(requestFactory(properties.connectTimeoutDuration(), properties.readTimeoutDuration()))
                .baseUrl(properties.baseUrl())
                .defaultHeaders(headers -> {
                    if (properties.authToken() != null && !properties.authToken().isBlank()) {
                        headers.setBearerAuth(properties.authToken());
                    }
                })
                .build();
    }

    @Bean
    public RestClient cloudflareRestClient(CloudflareClientProperties properties) {
        return RestClient.builder()
                .requestFactory(requestFactory(properties.connectTimeoutDuration(), properties.readTimeoutDuration()))
                .baseUrl(properties.baseUrl())
                .defaultHeaders(headers -> {
                    if (properties.authToken() != null && !properties.authToken().isBlank()) {
                        headers.setBearerAuth(properties.authToken());
                    }
                })
                .build();
    }

    private ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
