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

    @Getter
    @Setter
    public static class Jobs {
        private boolean pollingEnabled = false;

        @NotNull
        private Duration pollInterval = Duration.ofSeconds(30);

        @Min(1)
        private int batchSize = 10;
    }
}
