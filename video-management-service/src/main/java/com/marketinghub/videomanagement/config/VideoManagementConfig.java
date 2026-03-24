package com.marketinghub.videomanagement.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Beans auxiliares do serviço.
 */
@Configuration
public class VideoManagementConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient backendRestClient(RestClient.Builder builder,
                                        VideoManagementProperties properties) {
        RestClient.Builder configured = builder.baseUrl(properties.getBackendBaseUrl());
        if (StringUtils.hasText(properties.getAuthToken())) {
            configured = configured.defaultHeader("Authorization",
                    "Bearer " + properties.getAuthToken());
        }
        return configured.build();
    }
}
