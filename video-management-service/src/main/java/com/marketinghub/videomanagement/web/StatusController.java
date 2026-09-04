package com.marketinghub.videomanagement.web;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(StatusController.class);
    private final VideoManagementProperties properties;

    /** Configura a leitura sanitizada das propriedades operacionais do executor de vídeo. */
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
                "pdeAudiovisual", Map.of(
                        "enabled", properties.getPdeAudiovisual().isEnabled(),
                        "processCode", "pde-construction-approval",
                        "activityId", "audiovisual",
                        "executionResourceCode", "video-management-service"),
                "apolloPlanner", Map.of(
                        "enabled", properties.getApolloPlanner().isEnabled(),
                        "model", properties.getApolloPlanner().getModel(),
                        "apiKeyConfigured", credentialAvailable(
                                properties.getApolloPlanner().getApiKey(),
                                properties.getApolloPlanner().getApiKeyFile())),
                "providers", Map.of(
                        "real", Map.of(
                                "enabled", properties.getProviders().getReal().isEnabled(),
                                "acceptedNames", properties.getProviders().getReal().getAcceptedNames(),
                                "baseUrlConfigured", properties.getProviders().getReal().getBaseUrl() != null),
                        "veo", Map.of(
                                "enabled", properties.getProviders().getVeo().isEnabled(),
                                "acceptedNames", properties.getProviders().getVeo().getAcceptedNames(),
                                "apiKeyConfigured", credentialAvailable(
                                        properties.getProviders().getVeo().getApiKey(),
                                        properties.getProviders().getVeo().getApiKeyFile()),
                                "model", properties.getProviders().getVeo().getModel()),
                        "kling", Map.of(
                                "enabled", properties.getProviders().getKling().isEnabled(),
                                "acceptedNames", properties.getProviders().getKling().getAcceptedNames(),
                                "apiKeyConfigured", credentialAvailable(
                                        properties.getProviders().getKling().getApiKey(),
                                properties.getProviders().getKling().getApiKeyFile()),
                                "model", properties.getProviders().getKling().getModel()),
                        "editorialMotion", Map.of(
                                "enabled", properties.getProviders().getEditorialMotion().isEnabled(),
                                "acceptedNames",
                                properties.getProviders().getEditorialMotion().getAcceptedNames(),
                                "maxDurationSeconds",
                                properties.getProviders().getEditorialMotion().getMaxDurationSeconds(),
                                "providerCostUsd", 0),
                        "postProduction", Map.of(
                                "enabled", properties.getProviders().getPostProduction().isEnabled(),
                                "openAiTtsEnabled",
                                properties.getProviders().getPostProduction().isOpenAiTtsEnabled(),
                                "openAiApiKeyConfigured",
                                credentialAvailable(
                                        properties.getProviders().getPostProduction().getOpenAiApiKey(),
                                        properties.getProviders().getPostProduction().getOpenAiApiKeyFile()),
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
                                credentialAvailable(
                                        properties.getProviders().getLuma().getOpenAiApiKey(),
                                        properties.getProviders().getLuma().getOpenAiApiKeyFile()),
                                "openAiImageModel",
                                properties.getProviders().getLuma().getOpenAiImageModel(),
                                "openAiImageToolModel",
                                properties.getProviders().getLuma().getOpenAiImageToolModel()),
                        "runway", Map.of(
                                "enabled", properties.getProviders().getRunway().isEnabled(),
                                "acceptedNames", properties.getProviders().getRunway().getAcceptedNames(),
                                "apiKeyConfigured",
                                credentialAvailable(
                                        properties.getProviders().getRunway().getApiKey(),
                                        properties.getProviders().getRunway().getApiKeyFile()),
                                "model", properties.getProviders().getRunway().getModel(),
                                "durationSeconds", properties.getProviders().getRunway().getDurationSeconds()),
                        "heygen", Map.of(
                                "enabled", properties.getProviders().getHeygen().isEnabled(),
                                "acceptedNames", properties.getProviders().getHeygen().getAcceptedNames(),
                                "apiKeyConfigured",
                                credentialAvailable(
                                        properties.getProviders().getHeygen().getApiKey(),
                                        properties.getProviders().getHeygen().getApiKeyFile()),
                                "avatarIdConfigured", hasText(properties.getProviders().getHeygen().getAvatarId()),
                                "voiceIdConfigured", hasText(properties.getProviders().getHeygen().getVoiceId()),
                                "engineType", properties.getProviders().getHeygen().getEngineType()))
        );
    }

    /** Indica presença de texto sem retornar o valor configurado. */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Confirma a credencial direta ou um arquivo regular, legível e não vazio. */
    private boolean credentialAvailable(String directValue, String fileName) {
        if (hasText(directValue)) {
            return true;
        }
        if (!hasText(fileName)) {
            return false;
        }
        Path path = Path.of(fileName.trim());
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            return false;
        }
        try {
            return Files.size(path) > 0;
        } catch (IOException ex) {
            log.warn("Falha ao inspecionar arquivo de credencial; path={}", path, ex);
            return false;
        }
    }
}
