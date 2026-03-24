package com.marketinghub.videomanagement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configurações externas do serviço de gerenciamento de vídeo.
 */
@Data
@ConfigurationProperties(prefix = "video")
public class VideoManagementProperties {
    /** Base URL do backend/ads-service acessível a partir do container. */
    private String backendBaseUrl = "http://backend:8000";
    /** Token opcional para autenticação mútua entre serviços. */
    private String authToken;

    private Jobs jobs = new Jobs();

    @Data
    public static class Jobs {
        /** Habilita ou desabilita o polling automático. */
        private boolean pollingEnabled = false;
        /** Intervalo entre execuções do poller. */
        private Duration pollInterval = Duration.ofSeconds(30);
        /** Quantidade máxima de jobs buscados a cada ciclo. */
        private int batchSize = 10;
    }
}
