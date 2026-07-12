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

    @Getter
    @Setter
    public static class Providers {
        @NotNull
        private Real real = new Real();

        @NotNull
        private Veo veo = new Veo();
    }

    @Getter
    @Setter
    public static class Real {
        private boolean enabled = false;

        /**
         * Nomes que identificam o provider real dentro de providerName do job.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of("REAL", "HEYGEN", "SYNTHESIA"));

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

    @Getter
    @Setter
    public static class Veo {
        private boolean enabled = false;

        /**
         * Nomes que identificam jobs destinados ao adapter direto do VEO.
         */
        @NotNull
        private List<String> acceptedNames = new ArrayList<>(List.of("VEO", "VEO-3.1", "VEO_3_1", "REAL"));

        /**
         * Base URL oficial da Gemini API para geração de vídeos.
         */
        @NotNull
        private URI baseUrl = URI.create("https://generativelanguage.googleapis.com/v1beta");

        /**
         * Chave da Gemini API usada apenas pelo módulo executor de vídeo.
         */
        private String apiKey;

        private String model = "veo-3.1-generate-preview";
        private String aspectRatio = "9:16";
        private String resolution = "720p";
        private String personGeneration = "allow_all";
        private Integer durationSeconds = 8;

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(10);

        @Min(1)
        private int maxPollAttempts = 120;
    }
}
