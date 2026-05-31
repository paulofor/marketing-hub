package com.marketinghub.worker.geralanding.copy.request;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Responsabilidade: validar que o contrato de prompt/schema da etapa copy segue apenas o wireframe. */
class MontaRequestContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Garante que o schema da etapa copy não aceita blocos inventados fora do wireframe. */
    @Test
    void schemaShouldOnlyExposeBodySections() throws Exception {
        Map<String, Object> schema = readJson("prompts/geralanding/landing-page-copy-schema.json");

        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(schema.get("required")).isEqualTo(List.of("bodySections"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties).containsOnlyKeys("bodySections");
    }

    /** Garante que o prompt instrui a copy a não criar FAQ, checks, CTAs extras ou metadados. */
    @Test
    void promptShouldForbidExtraCopyBlocks() throws Exception {
        String prompt = readText("prompts/geralanding/landing-page-copy.md");

        assertThat(prompt)
                .contains("A raiz do JSON deve conter somente `bodySections`")
                .contains("É proibido devolver `faq`, `ctaBlocks`, `formMicrocopy`, `imageAccessibilityPlan`, `consistencyChecks`, `complianceNotes`, `pageGoal`, `primaryCTA`, `messageMatchSource`, `messageMatchNotes`")
                .contains("O wireframe é a única fonte de verdade estrutural")
                .contains("A etapa copy apenas escreve o valor `texto` para ids textuais existentes no wireframe");
    }

    /** Lê um recurso JSON do classpath e converte para mapa genérico. */
    private Map<String, Object> readJson(String path) throws Exception {
        return objectMapper.readValue(readText(path), new TypeReference<>() {});
    }

    /** Lê um recurso textual do classpath usando UTF-8. */
    private String readText(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
