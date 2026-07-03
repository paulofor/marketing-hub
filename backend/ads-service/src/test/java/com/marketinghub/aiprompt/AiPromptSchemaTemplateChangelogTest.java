package com.marketinghub.aiprompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Responsabilidade: proteger contratos Liquibase dos templates de prompt e schema de IA. */
class AiPromptSchemaTemplateChangelogTest {
    private static final Path PROMPT_SCHEMA_CHANGELOG = Path.of(
            "src/main/resources/db/changelog/changesets/2026-07-02-experiment-ai-prompt-schema-usage.yaml");

    /** Garante que a etapa Prova use o mesmo campo de evidencias aceito pelo AI Worker. */
    @Test
    void hypothesisProofTemplateShouldUseEvidenceSignals() throws IOException {
        String changelog = Files.readString(PROMPT_SCHEMA_CHANGELOG, StandardCharsets.UTF_8);
        String changeSetId = "2026-07-03-experiment-ai-prompt-schema-usage-proof-evidence-signals";
        String correctionChangeSet = changelog.substring(changelog.indexOf(changeSetId));

        assertThat(correctionChangeSet)
                .contains(changeSetId)
                .contains("\"evidenceSignals\"")
                .doesNotContain("\"proofSignals\"");
    }
}
