package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.openai.OpenAiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({WorkerProperties.class, OpenAiProperties.class})
public class WorkerConfig {
    @Bean
    RestClient restClient(WorkerProperties properties) {
        return RestClient.builder().baseUrl(properties.backendBaseUrl()).build();
    }
}
