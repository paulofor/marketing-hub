package com.marketinghub.opsmonitor.pipeline.healthcheck;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/** Habilita as propriedades e componentes da etapa de health check. */
@Configuration
@EnableConfigurationProperties(HealthCheckProperties.class)
public class HealthCheckConfiguration {

    /** Cria o cliente HTTP usado para chamadas aos módulos monitorados. */
    @Bean
    public HealthCheckProcessor healthCheckProcessor(WebClient.Builder builder) {
        return new HealthCheckProcessor(builder.build());
    }

    /** Cria o cliente HTTP apontado para o backend principal. */
    @Bean
    public HealthCheckBackendClient healthCheckBackendClient(WebClient.Builder builder,
            @Value("${ops-monitor.backend.base-url:http://191.252.181.168}") String backendBaseUrl) {
        return new HealthCheckBackendClient(builder.baseUrl(backendBaseUrl).build());
    }

    /** Cria o executor periódico que consome pendências e registra heartbeats. */
    @Bean
    public HealthCheckRunner healthCheckRunner(HealthCheckBackendClient backendClient,
            @Qualifier("healthCheckProcessor") HealthCheckProcessor processor, HealthCheckProperties properties) {
        return new HealthCheckRunner(backendClient, processor, properties);
    }
}
