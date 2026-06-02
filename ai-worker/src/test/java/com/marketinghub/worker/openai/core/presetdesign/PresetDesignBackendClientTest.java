package com.marketinghub.worker.openai.core.presetdesign;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.worker.openai.core.exception.OpenAiHttpException;
import com.marketinghub.worker.openai.core.model.OpenAiDispatch;
import com.marketinghub.worker.openai.core.model.StageExecution;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar o contrato HTTP do client design-preset do core OpenAI. */
class PresetDesignBackendClientTest {

    private MockWebServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Inicializa o backend simulado usado para capturar os payloads enviados pelo client. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    /** Encerra o backend simulado após cada teste do client. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Deve enviar prompt, schema e request cru no callback recebe-prompt. */
    @Test
    void markDispatchedShouldSendPromptSchemaAndRawRequestToRecebePrompt() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202));
        PresetDesignBackendClient client = new PresetDesignBackendClient(
                WebClient.builder(),
                new PresetDesignWorkerProperties(
                        true,
                        5,
                        server.url("/").toString(),
                        "/api",
                        "prompts/geralanding/landing-page-design-preset.md",
                        "prompts/geralanding/landing-page-design-preset-schema.json",
                        "experiment_pipeline_landing_page_design_preset",
                        Duration.ofSeconds(5)),
                objectMapper);
        StageExecution<PresetDesignInput> execution = new StageExecution<>(
                "job-ia-1",
                12L,
                "landing-page-design-preset",
                "INICIADO",
                Instant.parse("2026-05-29T10:00:00Z"),
                new PresetDesignInput(12L, "landing-page-design-preset", "job-ia-1", Map.of()));
        OpenAiDispatch dispatch = new OpenAiDispatch(
                "openai-job-1",
                "Prompt renderizado",
                "{\"type\":\"object\"}",
                "{\"model\":\"gpt-test\",\"input\":\"Prompt renderizado\"}",
                "# Prompt markdown bruto",
                Instant.parse("2026-05-29T10:01:00Z"));

        client.markDispatched(execution, dispatch);

        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/api/internal/geralanding/design-preset/stage-executions/job-ia-1/recebe-prompt");
        Map<String, Object> payload = objectMapper.readValue(request.getBody().readUtf8(), new TypeReference<>() {});
        assertThat(payload)
                .containsEntry("prompt", "Prompt renderizado")
                .containsEntry("promptMarkdownContent", "# Prompt markdown bruto")
                .containsEntry("schemaJson", "{\"type\":\"object\"}")
                .containsEntry("requestBodyJson", "{\"model\":\"gpt-test\",\"input\":\"Prompt renderizado\"}")
                .containsEntry("jobidopenai", "openai-job-1");
    }

    /** Deve enviar o payload bruto de erro da OpenAI no callback recebe-resposta quando a etapa falhar. */
    @Test
    void markFailedShouldSendOpenAiRawErrorPayloadToRecebeResposta() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(202));
        PresetDesignBackendClient client = new PresetDesignBackendClient(
                WebClient.builder(),
                new PresetDesignWorkerProperties(
                        true,
                        5,
                        server.url("/").toString(),
                        "/api",
                        "prompts/geralanding/landing-page-design-preset.md",
                        "prompts/geralanding/landing-page-design-preset-schema.json",
                        "experiment_pipeline_landing_page_design_preset",
                        Duration.ofSeconds(5)),
                objectMapper);
        StageExecution<PresetDesignInput> execution = new StageExecution<>(
                "job-ia-1",
                12L,
                "landing-page-design-preset",
                "INICIADO",
                Instant.parse("2026-05-29T10:00:00Z"),
                new PresetDesignInput(12L, "landing-page-design-preset", "job-ia-1", Map.of()));
        var error = new OpenAiHttpException(
                400,
                "{\n  \"error\": {\n    \"message\": \"Invalid schema for response_format 'experiment_pipeline_landing_page_design_preset': In context=(), 'allOf' is not permitted.\",\n    \"type\": \"invalid_request_error\",\n    \"param\": \"text.format.schema\",\n    \"code\": \"invalid_json_schema\"\n  }\n}",
                new RuntimeException("HTTP 400"));

        client.markFailed(execution, error);

        var request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath())
                .isEqualTo("/api/internal/geralanding/design-preset/stage-executions/job-ia-1/recebe-resposta");
        Map<String, Object> payload = objectMapper.readValue(request.getBody().readUtf8(), new TypeReference<>() {});
        assertThat(payload)
                .containsEntry("experimentId", 12)
                .containsEntry("stageCode", "landing-page-design-preset")
                .containsEntry("modelResponse", null)
                .containsEntry("openAiJobId", null);
        assertThat((String) payload.get("errorMessage"))
                .contains("OpenAI Responses API returned HTTP 400")
                .contains("Invalid schema for response_format")
                .contains("invalid_json_schema");
        assertThat((String) payload.get("errorDetail"))
                .contains("Invalid schema for response_format")
                .contains("'allOf' is not permitted")
                .contains("text.format.schema");
    }

    /** Deve carregar prompt e schema de presetdesign e montar request compatível com Structured Outputs. */
    @Test
    void promptBuilderShouldUsePresetDesignPromptAndSchemaResources() throws Exception {
        PresetDesignWorkerProperties properties = new PresetDesignWorkerProperties(
                true,
                5,
                server.url("/").toString(),
                "/api",
                "prompts/geralanding/landing-page-design-preset.md",
                "prompts/geralanding/landing-page-design-preset-schema.json",
                "experiment_pipeline_landing_page_design_preset",
                Duration.ofSeconds(5));
        var openAiProperties = new com.marketinghub.worker.openai.core.openai.OpenAiClientProperties(
                "test-key",
                "https://api.openai.com/v1",
                "gpt-test",
                Duration.ofMinutes(30),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true);
        PresetDesignPromptBuilder builder = new PresetDesignPromptBuilder(objectMapper, openAiProperties, properties);
        StageExecution<PresetDesignInput> execution = new StageExecution<>(
                "job-ia-1",
                12L,
                "landing-page-design-preset",
                "INICIADO",
                Instant.parse("2026-05-29T10:00:00Z"),
                new PresetDesignInput(
                        12L,
                        "landing-page-design-preset",
                        "job-ia-1",
                        Map.of(
                                "NICHE_NAME", "gestores",
                                "PAIN_JSON", Map.of("headline", "dor"),
                                "RESULT_JSON", Map.of("headline", "resultado"),
                                "landingPageWireframe", Map.of("pagina", Map.of("head", Map.of())))));

        var request = builder.build(execution);
        JsonNode body = objectMapper.readTree(request.requestBodyJson());

        assertThat(request.schemaName()).isEqualTo("experiment_pipeline_landing_page_design_preset");
        assertThat(request.prompt()).contains("gestores");
        assertThat(body.path("model").asText()).isEqualTo("gpt-test");
        assertThat(body.path("text").path("format").path("name").asText())
                .isEqualTo("experiment_pipeline_landing_page_design_preset");
        assertThat(body.path("text").path("format").path("strict").asBoolean()).isTrue();
    }

    /** Deve manter o schema de design-preset compatível com Structured Outputs estrito da OpenAI. */
    @Test
    void presetDesignSchemaShouldDeclareAdditionalPropertiesFalseForEveryObject() throws Exception {
        String schemaJson = new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json")
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode schema = objectMapper.readTree(schemaJson);
        List<String> invalidPaths = new ArrayList<>();

        collectObjectSchemasWithoutStrictAdditionalProperties(schema, "$", invalidPaths);

        assertThat(invalidPaths).isEmpty();
    }


    /** Deve manter required com todas as propriedades para cumprir Structured Outputs estrito. */
    @Test
    void presetDesignSchemaShouldRequireEveryDeclaredObjectProperty() throws Exception {
        String schemaJson = new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json")
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode schema = objectMapper.readTree(schemaJson);
        List<String> invalidPaths = new ArrayList<>();

        collectObjectSchemasWithoutCompleteRequired(schema, "$", invalidPaths);

        assertThat(invalidPaths).isEmpty();
    }

    /** Deve manter apenas pagina.corpo no contrato de design-preset, sem pagina.body duplicado. */
    @Test
    void presetDesignSchemaShouldUseOnlyCorpoAndRejectDuplicatedBodyField() throws Exception {
        String schemaJson = new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json")
                .getContentAsString(StandardCharsets.UTF_8);
        JsonNode schema = objectMapper.readTree(schemaJson);
        JsonNode pagina = schema.path("properties").path("pagina");
        JsonNode paginaProperties = pagina.path("properties");
        JsonNode corpo = paginaProperties.path("corpo");

        assertThat(paginaProperties.has("body")).isFalse();
        assertThat(pagina.path("required").toString()).isEqualTo("[\"head\",\"corpo\"]");
        assertThat(corpo.path("required").toString()).isEqualTo("[\"estilos\",\"secoes\"]");
        assertThat(corpo.path("properties").has("estilos")).isTrue();
        assertThat(corpo.path("properties").path("estilos").path("items").path("enum").toString())
                .isEqualTo("[\"bgBody\",\"fontBase\",\"textPrimary\",\"marginReset\"]");
    }

    /** Deve manter o schema de design-preset sem palavras-chave rejeitadas pela OpenAI Structured Outputs. */
    @Test
    void presetDesignSchemaShouldNotContainUnsupportedOpenAiStructuredOutputKeywords() throws Exception {
        String schemaJson = new ClassPathResource("prompts/geralanding/landing-page-design-preset-schema.json")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(schemaJson)
                .doesNotContain("\"allOf\"")
                .doesNotContain("\"if\"")
                .doesNotContain("\"then\"")
                .doesNotContain("\"not\"");
    }

    /** Percorre recursivamente o schema e lista objetos que não bloqueiam propriedades extras. */
    private void collectObjectSchemasWithoutStrictAdditionalProperties(JsonNode node, String path, List<String> invalidPaths) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            JsonNode type = node.path("type");
            if (isObjectSchemaType(type) && !hasExplicitFalseAdditionalProperties(node)) {
                invalidPaths.add(path);
            }
            node.fields().forEachRemaining(entry ->
                    collectObjectSchemasWithoutStrictAdditionalProperties(
                            entry.getValue(), path + "." + entry.getKey(), invalidPaths));
            return;
        }
        if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                collectObjectSchemasWithoutStrictAdditionalProperties(node.get(index), path + "[" + index + "]", invalidPaths);
            }
        }
    }


    /** Percorre recursivamente o schema e lista objetos cujo required não cobre todas as properties. */
    private void collectObjectSchemasWithoutCompleteRequired(JsonNode node, String path, List<String> invalidPaths) {
        if (node == null || node.isMissingNode()) {
            return;
        }
        if (node.isObject()) {
            if (isObjectSchemaType(node.path("type")) && !hasRequiredForEveryProperty(node)) {
                invalidPaths.add(path);
            }
            node.fields().forEachRemaining(entry ->
                    collectObjectSchemasWithoutCompleteRequired(entry.getValue(), path + "." + entry.getKey(), invalidPaths));
            return;
        }
        if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                collectObjectSchemasWithoutCompleteRequired(node.get(index), path + "[" + index + "]", invalidPaths);
            }
        }
    }

    /** Verifica se required contém exatamente todos os nomes de properties do objeto. */
    private boolean hasRequiredForEveryProperty(JsonNode node) {
        JsonNode properties = node.path("properties");
        JsonNode required = node.path("required");
        if (!properties.isObject() || !required.isArray()) {
            return false;
        }
        List<String> propertyNames = new ArrayList<>();
        properties.fieldNames().forEachRemaining(propertyNames::add);
        List<String> requiredNames = new ArrayList<>();
        required.forEach(item -> requiredNames.add(item.asText()));
        return requiredNames.containsAll(propertyNames) && propertyNames.containsAll(requiredNames);
    }

    /** Confirma que objetos declaram additionalProperties explicitamente como false. */
    private boolean hasExplicitFalseAdditionalProperties(JsonNode node) {
        JsonNode additionalProperties = node.path("additionalProperties");
        return additionalProperties.isBoolean() && !additionalProperties.asBoolean();
    }

    /** Identifica declarações de schema JSON para tipo object em formato textual ou lista de tipos. */
    private boolean isObjectSchemaType(JsonNode type) {
        if (type.isTextual()) {
            return "object".equals(type.asText());
        }
        if (type.isArray()) {
            for (JsonNode candidate : type) {
                if (candidate.isTextual() && "object".equals(candidate.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

}
