package com.marketinghub.pipelines.nichocnae.v3.personacandidategenerator;

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
                .contains("pipelines.nichocnae.v3:")
                .contains("persona-candidate-generator:")
                .contains("api-key: ${OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_API_KEY:${OPENAI_API_KEY:}}")
                .contains("api-key-file: ${OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_API_KEY_FILE:${OPENAI_API_KEY_FILE:/run/secrets/openai_api_key}}");
    }

    /** Confirma que o compose publicado monta o segredo OpenAI para a etapa v3 e para o fallback global. */
    @Test
    void shouldPublishOpenAiKeyFileFallbackInComposeFiles() throws IOException {
        String deployCompose = readProjectFile("docker-compose.deploy.yml");
        String localCompose = readProjectFile("docker-compose.yml");

        assertThat(deployCompose)
                .contains("OPENAI_API_KEY_FILE: ${OPENAI_API_KEY_FILE:-/run/secrets/openai_api_key}")
                .contains("OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_API_KEY_FILE: ${OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_API_KEY_FILE:-/run/secrets/openai_api_key}")
                .contains("OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_MODEL: ${OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_MODEL:-gpt-5.2}");
        assertThat(localCompose)
                .contains("OPENAI_API_KEY_FILE=${OPENAI_API_KEY_FILE:-/run/secrets/openai_api_key}")
                .contains("OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_API_KEY_FILE=${OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_API_KEY_FILE:-/run/secrets/openai_api_key}")
                .contains("OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_MODEL=${OPRM_NICHO_CNAE_V3_PERSONA_CANDIDATE_GENERATOR_OPENAI_MODEL:-gpt-5.2}");
    }

    /** Lê arquivos do projeto que ficam fora do classpath de teste. */
    private String readProjectFile(String relativePath) throws IOException {
        return java.nio.file.Files.readString(java.nio.file.Path.of(relativePath), StandardCharsets.UTF_8);
    }

}
