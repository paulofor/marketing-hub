package com.marketinghub.videomanagement.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar que o status operacional não aceite caminhos de secret falsos. */
class StatusControllerTest {

    /** Deve rejeitar diretório e aceitar somente arquivo não vazio como credencial de Apolo. */
    @Test
    void shouldReportOnlyReadableRegularSecretAsConfigured() throws Exception {
        Path directory = Files.createTempDirectory("apollo-secret-directory");
        Path secret = Files.createTempFile("apollo-secret", ".txt");
        try {
            Files.writeString(secret, "configured");
            VideoManagementProperties properties = new VideoManagementProperties();
            properties.getApolloPlanner().setApiKey("");
            properties.getApolloPlanner().setApiKeyFile(directory.toString());
            properties.getProviders().getKling().setApiKey("");
            properties.getProviders().getKling().setApiKeyFile(directory.toString());
            properties.getPdeAudiovisual().setEnabled(true);
            StatusController controller = new StatusController(properties);

            assertThat(apolloPlanner(controller.status()).get("apiKeyConfigured")).isEqualTo(false);
            assertThat(provider(controller.status(), "kling").get("apiKeyConfigured")).isEqualTo(false);

            properties.getApolloPlanner().setApiKeyFile(secret.toString());
            properties.getProviders().getKling().setApiKeyFile(secret.toString());
            assertThat(apolloPlanner(controller.status()).get("apiKeyConfigured")).isEqualTo(true);
            assertThat(apolloPlanner(controller.status()).get("model")).isEqualTo("gpt-5.6-sol");
            assertThat(provider(controller.status(), "kling").get("apiKeyConfigured")).isEqualTo(true);
            assertThat(provider(controller.status(), "kling").get("model")).isEqualTo("kling-v2-1-master");
            assertThat(provider(controller.status(), "editorialMotion"))
                    .containsEntry("maxDurationSeconds", 60)
                    .containsEntry("providerCostUsd", 0);
            assertThat(pdeAudiovisual(controller.status()))
                    .containsEntry("enabled", true)
                    .containsEntry("processCode", "pde-construction-approval")
                    .containsEntry("activityId", "audiovisual")
                    .containsEntry("executionResourceCode", "video-management-service");
        } finally {
            Files.deleteIfExists(secret);
            Files.deleteIfExists(directory);
        }
    }

    /** Extrai o bloco sanitizado do planejador no contrato de status. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> apolloPlanner(Map<String, Object> status) {
        return (Map<String, Object>) status.get("apolloPlanner");
    }

    /** Extrai um provedor sanitizado do contrato de status. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> provider(Map<String, Object> status, String providerName) {
        Map<String, Object> providers = (Map<String, Object>) status.get("providers");
        return (Map<String, Object>) providers.get(providerName);
    }

    /** Extrai o diagnóstico sanitizado do consumidor audiovisual BPM. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> pdeAudiovisual(Map<String, Object> status) {
        return (Map<String, Object>) status.get("pdeAudiovisual");
    }
}
