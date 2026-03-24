package com.marketinghub.videomanagement.web;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint simples para verificar a configuração ativa do serviço.
 */
@RestController
@RequestMapping("/api/status")
public class StatusController {
    private final VideoManagementProperties properties;

    public StatusController(VideoManagementProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public Map<String, Object> status() {
        return Map.of(
                "backendBaseUrl", properties.getBackendBaseUrl(),
                "pollingEnabled", properties.getJobs().isPollingEnabled(),
                "pollIntervalSeconds", properties.getJobs().getPollInterval().getSeconds(),
                "batchSize", properties.getJobs().getBatchSize()
        );
    }
}
