package com.marketinghub.mds.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BackendClientConfig {
    @Bean
    public RestClient backendRestClient(MdsProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getBackend().getBaseUrl())
                .build();
    }
}
