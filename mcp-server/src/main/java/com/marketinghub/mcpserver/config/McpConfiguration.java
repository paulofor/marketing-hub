package com.marketinghub.mcpserver.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(McpProperties.class)
public class McpConfiguration {

    @Bean
    public RestTemplate mcpRestTemplate(RestTemplateBuilder builder, McpProperties properties) {
        Duration timeout = Duration.ofMillis(properties.meta().requestTimeoutMillis());
        return builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }
}
