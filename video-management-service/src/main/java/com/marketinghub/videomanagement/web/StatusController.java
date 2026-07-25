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
                                "model", properties.getProviders().getVeo().getModel()),
                        "postProduction", Map.of(
                                "enabled", properties.getProviders().getPostProduction().isEnabled(),
                                "openAiTtsEnabled",
                                properties.getProviders().getPostProduction().isOpenAiTtsEnabled(),
                                "openAiApiKeyConfigured",
                                hasText(properties.getProviders().getPostProduction().getOpenAiApiKey())
                                        || hasText(properties.getProviders().getPostProduction().getOpenAiApiKeyFile()),
                                "openAiTtsModel",
                                properties.getProviders().getPostProduction().getOpenAiTtsModel(),
                                "openAiTtsVoice",
                                properties.getProviders().getPostProduction().getOpenAiTtsVoice()),
                        "luma", Map.of(
                                "enabled", properties.getProviders().getLuma().isEnabled(),
                                "acceptedNames", properties.getProviders().getLuma().getAcceptedNames(),
                                "openAiReferenceImageEnabled",
                                properties.getProviders().getLuma().isOpenAiReferenceImageEnabled(),
                                "openAiApiKeyConfigured",
                                hasText(properties.getProviders().getLuma().getOpenAiApiKey())
                                        || hasText(properties.getProviders().getLuma().getOpenAiApiKeyFile()),
                                "openAiImageModel",
                                properties.getProviders().getLuma().getOpenAiImageModel(),
                                "openAiImageToolModel",
                                properties.getProviders().getLuma().getOpenAiImageToolModel()),
                        "runway", Map.of(
                                "enabled", properties.getProviders().getRunway().isEnabled(),
                                "acceptedNames", properties.getProviders().getRunway().getAcceptedNames(),
                                "apiKeyConfigured",
                                hasText(properties.getProviders().getRunway().getApiKey())
                                        || hasText(properties.getProviders().getRunway().getApiKeyFile()),
                                "model", properties.getProviders().getRunway().getModel(),
                                "durationSeconds", properties.getProviders().getRunway().getDurationSeconds()),
                        "heygen", Map.of(
                                "enabled", properties.getProviders().getHeygen().isEnabled(),
                                "acceptedNames", properties.getProviders().getHeygen().getAcceptedNames(),
                                "apiKeyConfigured",
                                hasText(properties.getProviders().getHeygen().getApiKey())
                                        || hasText(properties.getProviders().getHeygen().getApiKeyFile()),
                                "avatarIdConfigured", hasText(properties.getProviders().getHeygen().getAvatarId()),
                                "voiceIdConfigured", hasText(properties.getProviders().getHeygen().getVoiceId()),
                                "engineType", properties.getProviders().getHeygen().getEngineType()))
        );
    }

    /** Indica presença de texto sem retornar o valor configurado. */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
