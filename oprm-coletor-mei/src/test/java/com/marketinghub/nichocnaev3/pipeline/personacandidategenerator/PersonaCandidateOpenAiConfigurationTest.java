package com.marketinghub.nichocnaev3.pipeline.personacandidategenerator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

/** Valida a configuração operacional da OpenAI para a etapa persona-candidate-generator v3. */
class PersonaCandidateOpenAiConfigurationTest {
    /** Confirma que a etapa v3 reutiliza o fallback global de chave OpenAI já usado pelas versões anteriores. */
    @Test
    void shouldUseGlobalOpenAiKeyFallbackFromApplicationConfiguration() throws IOException {
        String applicationYaml = StreamUtils.copyToString(
                new ClassPathResource("application.yml").getInputStream(), StandardCharsets.UTF_8);

        assertThat(applicationYaml)
                .contains("oprm:")
                .contains("nichocnaev3:")
                .contains("persona-candidate-generator:")
                .contains("api-key: ${OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_API_KEY:${OPENAI_API_KEY:}}")
                .contains("api-key-file: ${OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_API_KEY_FILE:${OPENAI_API_KEY_FILE:}}");
    }
}
