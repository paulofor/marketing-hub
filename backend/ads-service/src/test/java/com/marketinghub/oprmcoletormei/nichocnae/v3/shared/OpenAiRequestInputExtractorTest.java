package com.marketinghub.oprmcoletormei.nichocnae.v3.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/** Valida a extração do campo input em requests OpenAI auditados no NichoCNAE v3. */
class OpenAiRequestInputExtractorTest {
    private final OpenAiRequestInputExtractor extractor = new OpenAiRequestInputExtractor(new ObjectMapper());

    /** Deve extrair input textual simples da Responses API. */
    @Test
    void extractShouldReturnTextualInput() {
        String rawRequest = "{\"model\":\"gpt-5.2\",\"input\":\"# Prompt operacional\",\"service_tier\":\"flex\"}";

        String extracted = extractor.extract(rawRequest);

        assertThat(extracted).isEqualTo("# Prompt operacional");
    }

    /** Deve serializar input estruturado quando o request usar lista ou objeto. */
    @Test
    void extractShouldReturnStructuredInputAsJson() {
        String rawRequest = "{\"input\":[{\"role\":\"user\",\"content\":\"texto\"}]}";

        String extracted = extractor.extract(rawRequest);

        assertThat(extracted).isEqualTo("[{\"role\":\"user\",\"content\":\"texto\"}]");
    }

    /** Deve retornar nulo quando o payload não tiver input extraível. */
    @Test
    void extractShouldReturnNullWhenInputIsMissing() {
        assertThat(extractor.extract("{\"model\":\"gpt-5.2\"}")).isNull();
        assertThat(extractor.extract("texto sem json")).isNull();
    }
}
