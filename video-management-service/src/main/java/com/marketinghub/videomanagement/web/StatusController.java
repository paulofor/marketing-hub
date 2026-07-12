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

    /** Retorna configuração operacional sem expor credenciais sensíveis. */
    @GetMapping
    public Map<String, Object> status() {
        return Map.of(
                "backendBaseUrl", properties.getBackendBaseUrl(),
                "pollingEnabled", properties.getJobs().isPollingEnabled(),
                "pollIntervalSeconds", properties.getJobs().getPollInterval().getSeconds(),
                "batchSize", properties.getJobs().getBatchSize(),
                "providers", Map.of(
                        "real", Map.of(
                                "enabled", properties.getProviders().getReal().isEnabled(),
                                "acceptedNames", properties.getProviders().getReal().getAcceptedNames(),
                                "baseUrlConfigured", properties.getProviders().getReal().getBaseUrl() != null),
                        "veo", Map.of(
                                "enabled", properties.getProviders().getVeo().isEnabled(),
                                "acceptedNames", properties.getProviders().getVeo().getAcceptedNames(),
                                "apiKeyConfigured", hasText(properties.getProviders().getVeo().getApiKey()),
                                "model", properties.getProviders().getVeo().getModel()))
        );
    }

    /** Indica presença de texto sem retornar o valor configurado. */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
