package com.marketinghub.videomanagement.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Centraliza a configuração operacional do módulo executor de vídeos.
 */
@Getter
@Setter
@Component("videoManagementProperties")
@ConfigurationProperties(prefix = "video")
@Validated
public class VideoManagementProperties {

    @NotNull
    private URI backendBaseUrl = URI.create("http://backend:8000");

    private String authToken;

    /**
     * Identificador usado para registrar claim e heartbeat junto ao backend.
     */
    private String workerId = "video-management-service";

    @NotNull
    private Jobs jobs = new Jobs();

    @NotNull
    private Providers providers = new Providers();

    @NotNull
    private Storage storage = new Storage();

    /**
     * Define frequência e tamanho de lote do consumo de jobs no backend.
     */
    @Getter
    @Setter
    public static class Jobs {
        private boolean pollingEnabled = false;
        private boolean orphanRecoveryEnabled = true;

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(30);

        @NotNull
        private Duration orphanThreshold = Duration.ofMinutes(10);

        @NotNull
        private Duration backendCallBackoff = Duration.ofSeconds(2);

        @Min(1)
        private int batchSize = 10;

        @Min(1)
        private int backendCallMaxAttempts = 3;
    }

    /**
     * Agrupa configurações dos providers de renderização disponíveis.
     */
    @Getter
    @Setter
    public static class Providers {
        @NotNull
        private Real real = new Real();
    }

    /**
     * Configura a integração HTTP com o provider real de vídeo.
     */
    @Getter
    @Setter
    public static class Real {
        private boolean enabled = false;

        /**
         * Nomes que identificam o provider real dentro de providerName do job.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of("REAL", "VEO", "GEMINI_VEO", "HEYGEN", "SYNTHESIA"));

        /**
         * Base URL da API do provider real.
         */
        private URI baseUrl;

        /**
         * Token opcional para autenticação bearer na API do provider.
         */
        private String authToken;

        private String createPath = "/v1/renders";
        private String statusPathTemplate = "/v1/renders/{providerJobId}";

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(5);

        @Min(1)
        private int maxPollAttempts = 120;
    }

    /**
     * Configuração do bucket R2 usado para publicar vídeos finais por URL.
     */
    @Getter
    @Setter
    public static class Storage {
        private String bucket = "";
        private URI endpoint;
        private String publicBaseUrl = "";
        private String accessKeyId = "";
        private String secretAccessKey = "";
        private String region = "auto";
        private String rootFolder = "sales-videos";
    }
}
