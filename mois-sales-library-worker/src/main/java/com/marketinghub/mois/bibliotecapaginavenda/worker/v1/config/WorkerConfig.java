package com.marketinghub.mois.bibliotecapaginavenda.worker.v1.config;

import com.marketinghub.mois.bibliotecapaginavenda.worker.v1.pipeline.pageanalysis.OpenAiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Configura propriedades e clientes compartilhados do worker MOIS. */
@Configuration
@EnableConfigurationProperties({WorkerProperties.class, OpenAiProperties.class})
public class WorkerConfig {
    /** Cria o cliente HTTP base usado para chamadas ao backend principal. */
    @Bean
    RestClient restClient(WorkerProperties properties) {
        return RestClient.builder().baseUrl(properties.backendBaseUrl()).build();
    }
}
