package com.marketinghub.videomanagement.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Responsabilidade: validar os limites operacionais do comando Codex sombra de Apolo. */
class ApolloCodexShadowClientTest {
    /** Exige raciocínio máximo, sandbox somente leitura e schema sem bypass de aprovação. */
    @Test
    void shouldBuildReadOnlyCodexCommandWithoutSearchOrTools() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getApolloPlanner().getCodexShadow().setCommand("codex-test");
        ApolloCodexShadowClient client = new ApolloCodexShadowClient(properties, new ObjectMapper());

        List<String> command = client.command(Path.of("/tmp/out.json"), Path.of("/tmp/schema.json"));

        assertThat(command).contains("codex-test", "exec", "read-only", "approval_policy=\"never\"",
                "--output-schema", "/tmp/schema.json");
        assertThat(command).containsSubsequence("--config", "model_reasoning_effort=\"max\"");
        assertThat(command).doesNotContain("--search", "danger-full-access");
    }

    /** Bloqueia valores inferiores ou ausentes antes de iniciar o processo externo. */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "none", "low", "medium", "high", "xhigh"})
    void shouldRejectReasoningBelowMaximumBeforeStartingProcess(String effort) {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getApolloPlanner().getCodexShadow().setEnabled(true);
        properties.getApolloPlanner().getCodexShadow().setReasoningEffort(effort);
        properties.getApolloPlanner().getCodexShadow().setCommand("must-never-run");
        ApolloCodexShadowClient client = new ApolloCodexShadowClient(properties, new ObjectMapper());

        assertThatThrownBy(() -> client.plan(1L, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deve ser max");
        assertThatThrownBy(() -> client.command(Path.of("out"), Path.of("schema")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deve ser max");
    }
}
