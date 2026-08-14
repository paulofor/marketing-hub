package com.marketinghub.videomanagement.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Responsabilidade: validar os limites operacionais do comando Codex sombra de Apolo. */
class ApolloCodexShadowClientTest {
    /** Exige sandbox somente leitura, schema e ausência de pesquisa ou bypass de aprovação. */
    @Test
    void shouldBuildReadOnlyCodexCommandWithoutSearchOrTools() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getApolloPlanner().getCodexShadow().setCommand("codex-test");
        ApolloCodexShadowClient client = new ApolloCodexShadowClient(properties, new ObjectMapper());

        List<String> command = client.command(Path.of("/tmp/out.json"), Path.of("/tmp/schema.json"));

        assertThat(command).contains("codex-test", "exec", "read-only", "approval_policy=\"never\"",
                "--output-schema", "/tmp/schema.json");
        assertThat(command).doesNotContain("--search", "danger-full-access");
    }
}
