package com.marketinghub.worker.experimentpipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ExperimentPipelineOpenAiClientTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void prependsGlobalRulesAndCampaignAngleGuidanceForCampaignAngleSection() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                10L,
                "campaign-angle",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "system", "content": "System"},
                            {"role": "user", "content": "Prompt original de angulo"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(1).get("content"));
        assertThat(userPrompt).startsWith("Você cria ativos de campanha para o Marketing Hub.");
        assertThat(userPrompt).contains("Prompt original de angulo");
        assertThat(userPrompt).contains("SYSTEM_INSTRUCTIONS");
        assertThat(userPrompt).contains("CASE_DATA");
        assertThat(userPrompt).contains("[CASE_DATA_BEGIN]");
        assertThat(userPrompt).contains("OUTPUT_CONTRACT");
        assertThat(userPrompt).contains("- visualAngle");
        assertThat(userPrompt).contains("- proofSummary");
        assertThat(userPrompt).contains("- singleMindedPromise");
        assertThat(userPrompt).contains("- primaryCTA");
        assertThat(userPrompt).contains("- landingMatchLine");
    }

    @Test
    void prependsGlobalRulesOnlyForNonCampaignSections() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                10L,
                "ad-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt de anuncio"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).startsWith("Você cria ativos de campanha para o Marketing Hub.");
        assertThat(userPrompt).contains("Prompt de anuncio");
        assertThat(userPrompt).doesNotContain("proofSummary,");
    }

    @Test
    void prependsLandingCopyGuidanceForLandingCopySection() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                11L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).startsWith("Você cria ativos de campanha para o Marketing Hub.");
        assertThat(userPrompt).contains("Prompt da landing");
        assertThat(userPrompt).contains("SYSTEM_INSTRUCTIONS");
        assertThat(userPrompt).contains("CASE_DATA");
        assertThat(userPrompt).contains("OUTPUT_CONTRACT");
        assertThat(userPrompt).contains("- messageMatchSource");
        assertThat(userPrompt).contains("hero {");
        assertThat(userPrompt).contains("bodySections[]");
        assertThat(userPrompt).contains("ctaBlocks[]");
        assertThat(userPrompt).contains("consistencyChecks[]");
        assertThat(userPrompt).contains("complianceNotes");
        assertThat(userPrompt).contains("copy **não pode depender** de campos, `sectionId` ou estrutura do `landingPageWireframe`");
        assertThat(userPrompt).doesNotContain("compatível com os `sectionId` previstos no wireframe recebido em `CASE_DATA`");
    }

    @Test
    void prependsLandingLayoutGuidanceForLandingLayoutSection() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                12L,
                "landing-page-wireframe",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt do wireframe"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).startsWith("Você cria ativos de campanha para o Marketing Hub.");
        assertThat(userPrompt).contains("Prompt do wireframe");
        assertThat(userPrompt).contains("SYSTEM_INSTRUCTIONS");
        assertThat(userPrompt).contains("CASE_DATA");
        assertThat(userPrompt).contains("OUTPUT_CONTRACT");
        assertThat(userPrompt).contains("variantLayoutId");
        assertThat(userPrompt).contains("form-first");
        assertThat(userPrompt).contains("proof-first");
        assertThat(userPrompt).contains("mobilePriorityScore");
        assertThat(userPrompt).contains("dropOffRisk");
        assertThat(userPrompt).contains("sectionOrder");
        assertThat(userPrompt).contains("ctaSlot");
        assertThat(userPrompt).contains("readingFlowSpec");
        assertThat(userPrompt).contains("conversionPathSpec");
        assertThat(userPrompt).contains("proofPlan");
        assertThat(userPrompt).contains("Para evitar erro 422");
        assertThat(userPrompt).contains("copie os `sectionId` literalmente de `sectionOrder`");
        assertThat(userPrompt).contains("match 1:1");
        assertThat(userPrompt).contains("trustSignalsSpec");
        assertThat(userPrompt).contains("accessibilitySpec");
        assertThat(userPrompt).contains("maxParagraphLinesMobile <= 4");
        assertThat(userPrompt).contains("bulletDensityPerSection >= 3");
    }

    @Test
    void prependsLandingImagePlanningGuidanceAlignedWithCanonicalContract() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                13L,
                "landing-page-image-planning",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt de imagens"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).contains("Responda em JSON válido e estritamente aderente ao artefato `landingPageImagePlanning`.");
        assertThat(userPrompt).contains("Campos obrigatórios:");
        assertThat(userPrompt).contains("- generationPrompt");
        assertThat(userPrompt).contains("A etapa `landingPageImagePlanning` é responsável somente por gerar o prompt final de execução para o modelo de imagem.");
        assertThat(userPrompt).contains("Os bindings estruturais de imagem (por seção) são definidos no `landingPageWireframe` e apenas consumidos nesta etapa.");
    }

    @Test
    void prependsLandingDesignPresetGuidanceWithLhmRuntimeBlock() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                15L,
                "landing-page-design-preset",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt do design preset"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).contains("landingPageDesignPreset");
        assertThat(userPrompt).contains("lhmRuntime");
        assertThat(userPrompt).contains("baseCss");
        assertThat(userPrompt).contains("cssVersion");
        assertThat(userPrompt).contains("cssNotes");
    }

    @Test
    void prependsLandingHtmlGuidanceWithExplicitBindingChecklist() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(
                        payloadRef,
                        """
                                <!doctype html>
                                <html lang="pt-BR">
                                  <body>
                                    <form id="lead-capture-primary"></form>
                                  </body>
                                </html>
                                """)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                14L,
                "landing-page-html",
                "gpt-5.1-codex",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt de html"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).contains("surfaceMatrix[]");
        assertThat(userPrompt).contains("imageMatrix[]");
        assertThat(userPrompt).contains("data-image-section-id");
        assertThat(userPrompt).contains("data-image-binding-key");
        assertThat(userPrompt).contains("missingSections");
        assertThat(userPrompt).contains("extraSections");
        assertThat(userPrompt).contains("missingImageBindings");
        assertThat(userPrompt).contains("extraImageBindings");
        assertThat(userPrompt).contains("só finalize quando `missingSections=[]`, `extraSections=[]`, `missingImageBindings=[]` e `extraImageBindings=[]`");
        assertThat(userPrompt).contains("case-sensitive");
    }

    @Test
    void injectsStructuredCaseDataBlockIntoSectionTemplate() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                121L,
                "campaign-angle",
                "gpt-5.2",
                """
                        NICHE_NAME: E-commerce
                        PRIMARY_CTA_LABEL: Quero minha análise
                        """,
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt com variaveis"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).contains("CASE_DATA");
        assertThat(userPrompt).contains("[CASE_DATA_BEGIN]");
        assertThat(userPrompt).contains("NICHE_NAME: E-commerce");
        assertThat(userPrompt).contains("PRIMARY_CTA_LABEL: Quero minha análise");
        assertThat(userPrompt).contains("[CASE_DATA_END]");
    }

    @Test
    void normalizesJsonSchemaRequiredForStrictResponsesApiCompatibility() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                13L,
                "ad-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt de anuncio"}
                          ],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object",
                                "additionalProperties": false,
                                "properties": {
                                  "adCopy": {
                                    "type": "object",
                                    "additionalProperties": false,
                                    "properties": {
                                      "primaryText": {"type": "string"},
                                      "headline": {"type": "string"}
                                    },
                                    "required": ["headline"]
                                  }
                                },
                                "required": ["adCopy"]
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) payload.get("text");
        @SuppressWarnings("unchecked")
        Map<String, Object> format = (Map<String, Object>) text.get("format");
        assertThat(format.get("name")).isEqualTo("experiment_pipeline_ad_copy");
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) format.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> adCopy = (Map<String, Object>) properties.get("adCopy");
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) adCopy.get("required");
        assertThat(required).contains("headline", "primaryText");
    }

    @Test
    void enforcesAdditionalPropertiesFalseForNestedObjectSchemas() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                15L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing"}
                          ],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object",
                                "properties": {
                                  "landingPageCopy": {
                                    "type": "object",
                                    "properties": {
                                      "messageMatchSource": {"type": "string"}
                                    }
                                  }
                                }
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) payload.get("text");
        @SuppressWarnings("unchecked")
        Map<String, Object> format = (Map<String, Object>) text.get("format");
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) format.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> landingPageCopy = (Map<String, Object>) properties.get("landingPageCopy");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(landingPageCopy.get("additionalProperties")).isEqualTo(false);
    }


    @Test
    void keepsRequiredAlignedWithPropertiesWhenSchemaContainsLegacyKeys() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                16L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing"}
                          ],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object",
                                "properties": {
                                  "copy": {
                                    "type": "string"
                                  }
                                },
                                "required": ["landingPageCopy"]
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) payload.get("text");
        @SuppressWarnings("unchecked")
        Map<String, Object> format = (Map<String, Object>) text.get("format");
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) format.get("schema");
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertThat(required).containsExactly("copy");
    }

    @Test
    void normalizesJsonSchemaInsideResponseFormatWrapper() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                18L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing"}
                          ],
                          "response_format": {
                            "type": "json_schema",
                            "json_schema": {
                              "schema": {
                                "type": "object",
                                "properties": {
                                  "landingPageCopy": {"type": "object"}
                                }
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> responseFormat = (Map<String, Object>) payload.get("response_format");
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonSchemaWrapper = (Map<String, Object>) responseFormat.get("json_schema");
        assertThat(jsonSchemaWrapper.get("name")).isEqualTo("experiment_pipeline_landing_page_copy");
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) jsonSchemaWrapper.get("schema");
        assertThat(schema.get("additionalProperties")).isEqualTo(false);
        assertThat(schema.get("required")).isEqualTo(java.util.List.of("landingPageCopy"));
    }

    @Test
    void forcesClosedObjectSchemasEvenWhenNoPropertiesExist() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                19L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing"}
                          ],
                          "text": {
                            "format": {
                              "type": "json_schema",
                              "schema": {
                                "type": "object",
                                "properties": {
                                  "landingPageCopy": {
                                    "type": "object"
                                  }
                                }
                              }
                            }
                          }
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) payload.get("text");
        @SuppressWarnings("unchecked")
        Map<String, Object> format = (Map<String, Object>) text.get("format");
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) format.get("schema");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> landingPageCopy = (Map<String, Object>) properties.get("landingPageCopy");
        assertThat(landingPageCopy.get("additionalProperties")).isEqualTo(false);
        assertThat(landingPageCopy.get("required")).isEqualTo(java.util.List.of());
        assertThat(landingPageCopy.get("properties")).isEqualTo(java.util.Map.of());
    }

    @Test
    void preservesLandingPageHtmlWithoutWorkerNormalizationWhenModelReturnsDynamicFormOnly() throws Exception {
        String openAiText = MAPPER.writeValueAsString(Map.of(
                "landingPageHtml", Map.of(
                        "htmlDocument", """
                                <!doctype html>
                                <html lang="pt-BR">
                                <body>
                                  <form id="lead-capture-primary">
                                    <div id="form-fields"></div>
                                  </form>
                                </body>
                                </html>
                                """,
                        "summary", "ok",
                        "consistencyChecks", List.of()
                )));

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                20L,
                "landing-page-html",
                "gpt-5.1-codex",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing html"}
                          ]
                        }
                        """,
                Instant.now());

        ExperimentPipelineJobCompletionPayload payload = client.generate(job);
        Map<String, Object> content = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> landingPageHtml = (Map<String, Object>) content.get("landingPageHtml");
        String htmlDocument = String.valueOf(landingPageHtml.get("htmlDocument")).toLowerCase();

        assertThat(htmlDocument).contains("<div id=\"form-fields\"></div>");
        assertThat(htmlDocument).contains("id=\"lead-capture-primary\"");
        assertThat(htmlDocument).doesNotContain("name=\"email\"");
        assertThat(htmlDocument).doesNotContain("name=\"whatsapp\"");
        assertThat(htmlDocument).doesNotContain("id=\"lead-capture-submit-contract\"");
    }

    @Test
    void preservesLandingPageHtmlWhenSectionComesAsEnumName() throws Exception {
        String openAiText = MAPPER.writeValueAsString(Map.of(
                "landingPageHtml", Map.of(
                        "htmlDocument", """
                                <!doctype html>
                                <html lang="pt-BR">
                                <body>
                                  <form id="lead-capture-primary">
                                    <div class="field"><label>Nome</label><input type="text" name="nome" required /></div>
                                    <div class="field"><label>Objetivo principal</label><select name="objetivo"><option>A</option></select></div>
                                  </form>
                                </body>
                                </html>
                                """,
                        "summary", "resumo antigo",
                        "consistencyChecks", List.of(Map.of("check", "FORM_USABILITY", "status", "PASS", "details", "Objetivo principal obrigatório"))
                )));

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                20L,
                "LANDING_PAGE_HTML",
                "gpt-5.1-codex",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing html"}
                          ]
                        }
                        """,
                Instant.now());

        ExperimentPipelineJobCompletionPayload payload = client.generate(job);
        Map<String, Object> content = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> landingPageHtml = (Map<String, Object>) content.get("landingPageHtml");
        String htmlDocument = String.valueOf(landingPageHtml.get("htmlDocument")).toLowerCase();

        assertThat(htmlDocument).contains("name=\"nome\"");
        assertThat(htmlDocument).contains("name=\"objetivo\"");
        assertThat(htmlDocument).contains("<select name=\"objetivo\"");
        assertThat(htmlDocument).doesNotContain("id=\"lead-capture-submit-contract\"");
    }

    @Test
    void acceptsRawHtmlResponseForLandingPageHtmlSection() throws Exception {
        String openAiText = """
                <!doctype html>
                <html lang="pt-BR">
                <body>
                  <form id="lead-capture-primary">
                    <div class="field"><label>Nome</label><input type="text" name="nome" required /></div>
                    <div class="field"><label>Objetivo principal</label><select name="objetivo"><option>A</option></select></div>
                  </form>
                </body>
                </html>
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                21L,
                "LANDING_PAGE_HTML",
                "gpt-5.1-codex",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing html"}
                          ]
                        }
                        """,
                Instant.now());

        ExperimentPipelineJobCompletionPayload payload = client.generate(job);
        Map<String, Object> content = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> landingPageHtml = (Map<String, Object>) content.get("landingPageHtml");
        String htmlDocument = String.valueOf(landingPageHtml.get("htmlDocument")).toLowerCase();

        assertThat(htmlDocument).contains("name=\"nome\"");
        assertThat(htmlDocument).contains("name=\"objetivo\"");
        assertThat(htmlDocument).doesNotContain("name=\"email\"");
        assertThat(htmlDocument).doesNotContain("name=\"whatsapp\"");
    }

    @Test
    void acceptsMarkdownHtmlCodeBlockForLandingPageHtmlSection() throws Exception {
        String openAiText = """
                ```html
                <!doctype html>
                <html lang="pt-BR">
                <body>
                  <form id="lead-capture-primary">
                    <div class="field"><label>Nome</label><input type="text" name="nome" required /></div>
                  </form>
                </body>
                </html>
                ```
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                22L,
                "LANDING_PAGE_HTML",
                "gpt-5.1-codex",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing html"}
                          ]
                        }
                        """,
                Instant.now());

        ExperimentPipelineJobCompletionPayload payload = client.generate(job);
        Map<String, Object> content = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> landingPageHtml = (Map<String, Object>) content.get("landingPageHtml");
        String htmlDocument = String.valueOf(landingPageHtml.get("htmlDocument")).toLowerCase();

        assertThat(htmlDocument).contains("<!doctype html>");
        assertThat(htmlDocument).contains("name=\"nome\"");
        assertThat(htmlDocument).doesNotContain("name=\"email\"");
        assertThat(htmlDocument).doesNotContain("name=\"whatsapp\"");
    }

    @Test
    void acceptsLandingPageHtmlWithoutJsonEnvelope() throws Exception {
        String openAiText = """
                <!doctype html>
                <html lang="pt-BR">
                <head><meta charset="UTF-8"></head>
                <body><form id="lead-capture-primary"><input type="text" name="nome" /></form></body>
                </html>
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                29L,
                "LANDING_PAGE_HTML",
                "gpt-5.1-codex",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing html"}
                          ]
                        }
                        """,
                Instant.now());

        ExperimentPipelineJobCompletionPayload payload = client.generate(job);
        Map<String, Object> content = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> landingPageHtml = (Map<String, Object>) content.get("landingPageHtml");
        String htmlDocument = String.valueOf(landingPageHtml.get("htmlDocument")).toLowerCase();

        assertThat(htmlDocument).contains("<!doctype html>");
        assertThat(htmlDocument).contains("name=\"nome\"");
        assertThat(htmlDocument).doesNotContain("name=\"email\"");
        assertThat(htmlDocument).doesNotContain("name=\"whatsapp\"");
    }

    @Test
    void failsWhenLandingPageHtmlContractIsBrokenWithoutHtmlDocument() throws Exception {
        String openAiText = """
                {
                  "landingPageHtml": {
                    "summary": "sem html"
                  }
                }
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                23L,
                "LANDING_PAGE_HTML",
                "gpt-5.1-codex",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing html"}
                          ]
                        }
                        """,
                Instant.now());

        Throwable error = catchThrowable(() -> client.generate(job));

        assertThat(error).isInstanceOf(IllegalStateException.class);
        assertThat(error).hasMessageContaining("Falha ao gerar seção LANDING_PAGE_HTML do experimento 23");
        assertThat(error.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LANDING_PAGE_HTML sem campo htmlDocument válido");
    }

    @Test
    void failsWhenLandingPageHtmlContainsSerializedJsonInsideHtmlDocument() throws Exception {
        String openAiText = """
                {
                  "landingPageHtml": {
                    "htmlDocument": "{\\"headline\\":\\"Plano de Conteúdo\\",\\"cta\\":\\"Quero a prévia\\"}"
                  }
                }
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                30L,
                "LANDING_PAGE_HTML",
                "gpt-5.1-codex",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing html"}
                          ]
                        }
                        """,
                Instant.now());

        Throwable error = catchThrowable(() -> client.generate(job));

        assertThat(error).isInstanceOf(IllegalStateException.class);
        assertThat(error).hasMessageContaining("Falha ao gerar seção LANDING_PAGE_HTML do experimento 30");
        assertThat(error.getCause())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LANDING_PAGE_HTML exige HTML puro");
    }

    @Test
    void enforcesGpt52ModelForEveryPipelineCall() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                14L,
                "ad-copy",
                "gpt-4o-mini",
                "prompt",
                """
                        {
                          "model": "gpt-4o-mini",
                          "input": [
                            {"role": "user", "content": "Prompt de anuncio"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        assertThat(payload.get("model")).isEqualTo("gpt-5.2");
    }

    @Test
    void enforcesGpt51CodexModelForLandingHtmlPipelineCall() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(
                        payloadRef,
                        """
                                <!doctype html>
                                <html lang="pt-BR">
                                  <body>
                                    <form id="lead-capture-primary"></form>
                                  </body>
                                </html>
                                """)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                15L,
                "landing-page-html",
                "gpt-4o-mini",
                "prompt",
                """
                        {
                          "model": "gpt-4o-mini",
                          "input": [
                            {"role": "user", "content": "Prompt de landing html"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        assertThat(payload.get("model")).isEqualTo("gpt-5.1-codex");
    }

    @Test
    void enforcesGpt51CodexModelForLandingHtmlPipelineCallWhenSectionIsEnumName() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(
                        payloadRef,
                        """
                                <!doctype html>
                                <html lang="pt-BR">
                                  <body>
                                    <form id="lead-capture-primary"></form>
                                  </body>
                                </html>
                                """)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                16L,
                "LANDING_PAGE_HTML",
                "gpt-5.1-codex",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt de landing html"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        assertThat(payload.get("model")).isEqualTo("gpt-5.1-codex");
    }

    @Test
    void storesTemplateTraceInTrackedRequestBodyForLandingCopy() throws Exception {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                22L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt da landing"}
                          ]
                        }
                        """,
                Instant.now());

        ExperimentPipelineJobCompletionPayload completionPayload = client.generate(job);
        Map<String, Object> trackedRequest = MAPPER.readValue(completionPayload.requestBodyJson(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> templateTrace = (Map<String, Object>) trackedRequest.get("templateTrace");
        assertThat(templateTrace)
                .containsEntry("template_id", "landing-copy")
                .containsEntry("template_version", "v2")
                .containsEntry("artifact_target", "landingPageCopy")
                .containsEntry("model", "gpt-5.2");
    }

    @Test
    void stripsTemplateHeaderFromPromptSentToOpenAi() {
        AtomicReference<Map<String, Object>> payloadRef = new AtomicReference<>();
        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(payloadRef)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobDto job = new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                23L,
                "campaign-angle",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [
                            {"role": "user", "content": "Prompt base"}
                          ]
                        }
                        """,
                Instant.now());

        client.generate(job);

        Map<String, Object> payload = payloadRef.get();
        @SuppressWarnings("unchecked")
        var input = (java.util.List<Map<String, Object>>) payload.get("input");
        String userPrompt = String.valueOf(input.get(0).get("content"));
        assertThat(userPrompt).doesNotContain("template_id:");
        assertThat(userPrompt).doesNotContain("template_version:");
        assertThat(userPrompt).doesNotContain("artifact_target:");
    }

    @Test
    void computesPhase4ChecksWithFailForGenericOfferAndVagueCta() throws Exception {
        String openAiText = MAPPER.writeValueAsString(Map.of(
                "landingPageCopy", Map.of(
                        "pageGoal", "Capturar lead",
                        "messageMatchSource", "anuncio",
                        "messageMatchNotes", "continuidade simples",
                        "primaryCTA", "Saiba mais",
                        "complianceNotes", "Entrega digital",
                        "hero", Map.of("headline", "Receba um kit", "ctaLabel", "Saiba mais"),
                        "bodySections", List.of(Map.of(
                                "sectionId", "offer-1",
                                "title", "Kit completo",
                                "summary", "ativos digitais",
                                "copy", "Receba um kit com ativos digitais")),
                        "ctaBlocks", List.of(Map.of("ctaLabel", "Saiba mais")),
                        "faq", List.of(),
                        "consistencyChecks", List.of()
                )));

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobCompletionPayload payload = client.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                30L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [{"role": "user", "content": "Prompt copy"}]
                        }
                        """,
                Instant.now()));

        Map<String, Object> content = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> copy = (Map<String, Object>) content.get("landingPageCopy");
        assertThat(readCheckStatus(copy, "DELIVERABLE_CLARITY")).isEqualTo("FAIL");
        assertThat(readCheckStatus(copy, "CTA_SPECIFICITY")).isEqualTo("FAIL");
        assertThat(readCheckStatus(copy, "ARTIFACT_SCHEMA_COMPATIBILITY")).isEqualTo("WARN");
    }

    @Test
    void computesPhase4ChecksWithPassForConcreteCopy() throws Exception {
        String openAiText = MAPPER.writeValueAsString(Map.of(
                "landingPageCopy", Map.of(
                        "pageGoal", "Gerar leads com preview",
                        "messageMatchSource", "headline anuncio",
                        "messageMatchNotes", "os templates ajudam a reduzir retrabalho e aumentar conversão",
                        "primaryCTA", "Quero receber a prévia do checklist",
                        "complianceNotes", "Entrega 100% digital",
                        "hero", Map.of("headline", "Checklist + template", "ctaLabel", "Quero receber a prévia do checklist"),
                        "bodySections", List.of(Map.of(
                                "sectionId", "offer-1",
                                "title", "O que você recebe",
                                "summary", "Checklist, template e roteiro para aumentar conversão",
                                "copy", "Você recebe checklist, template e roteiro para reduzir erros e aumentar conversão.",
                                "bullets", List.of("Checklist de oferta", "Template de anúncio", "Roteiro de revisão"))),
                        "ctaBlocks", List.of(Map.of("ctaLabel", "Quero receber a prévia do checklist")),
                        "faq", List.of(Map.of("question", "?", "answer", "!", "objectionTag", "tempo")),
                        "consistencyChecks", List.of()
                )));

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");

        ExperimentPipelineJobCompletionPayload payload = client.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(),
                31L,
                "landing-page-copy",
                "gpt-5.2",
                "prompt",
                """
                        {
                          "model": "gpt-5.2",
                          "input": [{"role": "user", "content": "Prompt copy"}]
                        }
                        """,
                Instant.now()));

        Map<String, Object> content = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> copy = (Map<String, Object>) content.get("landingPageCopy");
        assertThat(readCheckStatus(copy, "DELIVERABLE_CLARITY")).isEqualTo("PASS");
        assertThat(readCheckStatus(copy, "DELIVERABLE_TO_OUTCOME_LINK")).isEqualTo("PASS");
        assertThat(readCheckStatus(copy, "CTA_SPECIFICITY")).isEqualTo("PASS");
    }

    @Test
    void computesPhase4ChecksWithWarnAndFailAcrossWireframeImagePlanningAndHtml() throws Exception {
        String openAiText = MAPPER.writeValueAsString(Map.of(
                "landingPageWireframe", Map.of(
                        "pageGoal", "captura",
                        "variantLayoutId", "form-first",
                        "sectionOrder", List.of(
                                Map.of("sectionId", "hero", "surfaceSpec", Map.of("surfaceToken", "base", "style", "solid", "contrastMode", "normal")),
                                Map.of("sectionId", "proof", "surfaceSpec", Map.of("surfaceToken", "base", "style", "solid", "contrastMode", "normal")),
                                Map.of("sectionId", "cta", "surfaceSpec", Map.of("surfaceToken", "base", "style", "solid", "contrastMode", "normal"))),
                        "consistencyChecks", List.of()
                )));
        ExperimentPipelineOpenAiClient wireframeClient = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), openAiText)),
                MAPPER,
                "test-key",
                "http://openai");
        ExperimentPipelineJobCompletionPayload wireframePayload = wireframeClient.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(), 32L, "landing-page-wireframe", "gpt-5.2", "prompt",
                "{\"model\":\"gpt-5.2\",\"input\":[{\"role\":\"user\",\"content\":\"prompt\"}]}", Instant.now()));
        Map<String, Object> wireframeContent = MAPPER.readValue(wireframePayload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> wireframe = (Map<String, Object>) wireframeContent.get("landingPageWireframe");
        assertThat(readCheckStatus(wireframe, "SECTION_THEME_VARIATION")).isEqualTo("FAIL");

        String imageText = MAPPER.writeValueAsString(Map.of(
                "landingPageImagePlanning", Map.of(
                        "pageGoal", "preview",
                        "images", List.of(Map.of(
                                "sectionId", "proof",
                                "sectionName", "Prova",
                                "imageRole", "preview",
                                "conversionRole", "proof",
                                "imagePrompt", "imagem simples",
                                "supportingElements", List.of())),
                        "consistencyChecks", List.of()
                )));
        ExperimentPipelineOpenAiClient imageClient = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), imageText)),
                MAPPER,
                "test-key",
                "http://openai");
        ExperimentPipelineJobCompletionPayload imagePayload = imageClient.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(), 33L, "landing-page-image-planning", "gpt-5.2", "prompt",
                "{\"model\":\"gpt-5.2\",\"input\":[{\"role\":\"user\",\"content\":\"prompt\"}]}", Instant.now()));
        Map<String, Object> imageContent = MAPPER.readValue(imagePayload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> imagePlanning = (Map<String, Object>) imageContent.get("landingPageImagePlanning");
        assertThat(readCheckStatus(imagePlanning, "PREVIEW_CONCRETENESS")).isEqualTo("FAIL");

        String htmlText = MAPPER.writeValueAsString(Map.of(
                "landingPageHtml", Map.of(
                        "htmlDocument", """
                                <html><body>
                                  <section class="surface-base" data-surface-token="base"></section>
                                  <section class="surface-base" data-surface-token="base"></section>
                                  <form id="lead-capture-primary"></form>
                                </body></html>
                                """,
                        "summary", "ok",
                        "formSpec", Map.of("formId", "lead-capture-primary"),
                        "imagePlacementContract", Map.of("requiredDataAttributes", List.of()),
                        "consistencyChecks", List.of()
                )));
        ExperimentPipelineOpenAiClient htmlClient = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), htmlText)),
                MAPPER,
                "test-key",
                "http://openai");
        ExperimentPipelineJobCompletionPayload htmlPayload = htmlClient.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(), 34L, "landing-page-html", "gpt-5.1-codex", "prompt",
                "{\"model\":\"gpt-5.2\",\"input\":[{\"role\":\"user\",\"content\":\"prompt\"}]}", Instant.now()));
        Map<String, Object> htmlContent = MAPPER.readValue(htmlPayload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> html = (Map<String, Object>) htmlContent.get("landingPageHtml");
        assertThat(readCheckStatus(html, "SECTION_THEME_VARIATION")).isEqualTo("FAIL");
        assertThat(readCheckStatus(html, "PREVIEW_CONCRETENESS")).isEqualTo("WARN");
    }

    @Test
    void synchronizesLandingHtmlSurfaceAttributesFromWireframeBeforeCompletion() throws Exception {
        String htmlText = """
                {
                  "landingPageHtml": {
                    "htmlDocument": "<!doctype html><html><body><section data-section-id='hero'></section><section data-section-id='proof' data-surface-token='wrong' data-surface-style='wrong' data-surface-contrast='wrong'></section></body></html>",
                    "summary": "ok",
                    "formSpec": {"formId": "lead-capture-primary"},
                    "imagePlacementContract": {"requiredDataAttributes": []},
                    "consistencyChecks": []
                  }
                }
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), htmlText)),
                MAPPER,
                "test-key",
                "http://openai");

        String requestBody = """
                {
                  "model":"gpt-5.2",
                  "input":[
                    {"role":"user","content":"Prompt\\nWireframe da landing:\\n{\\"landingPageWireframe\\":{\\"sectionOrder\\":[{\\"sectionId\\":\\"hero\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-hero\\",\\"style\\":\\"band\\",\\"contrastMode\\":\\"high\\"}},{\\"sectionId\\":\\"proof\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-proof\\",\\"style\\":\\"solid\\",\\"contrastMode\\":\\"normal\\"}}]}}\\nPlanejamento de imagens da landing:\\n{}"}
                  ]
                }
                """;

        ExperimentPipelineJobCompletionPayload payload = client.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(), 35L, "landing-page-html", "gpt-5.1-codex", "prompt", requestBody, Instant.now()));

        Map<String, Object> htmlContent = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> html = (Map<String, Object>) htmlContent.get("landingPageHtml");
        String htmlDocument = String.valueOf(html.get("htmlDocument"));
        assertThat(htmlDocument).contains("data-section-id='hero' data-surface-token=\"surface-hero\" data-surface-style=\"band\" data-surface-contrast=\"high\"");
        assertThat(htmlDocument).contains("data-section-id='proof' data-surface-token=\"surface-proof\" data-surface-style=\"solid\" data-surface-contrast=\"normal\"");
    }

    @Test
    void synchronizesLandingHtmlSurfaceAttributesUsingDesignPresetWhenWireframeOmitsStyleAndContrast() throws Exception {
        String htmlText = """
                {
                  "landingPageHtml": {
                    "htmlDocument": "<!doctype html><html><body><section data-section-id='hero'></section><section data-section-id='proof'></section></body></html>",
                    "summary": "ok",
                    "formSpec": {"formId": "lead-capture-primary"},
                    "imagePlacementContract": {"requiredDataAttributes": []},
                    "consistencyChecks": []
                  }
                }
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), htmlText)),
                MAPPER,
                "test-key",
                "http://openai");

        String requestBody = """
                {
                  "model":"gpt-5.2",
                  "input":[
                    {"role":"user","content":"Prompt v2\\n1) Wireframe aprovado (JSON):\\n{\\"landingPageWireframe\\":{\\"sectionOrder\\":[{\\"sectionId\\":\\"hero\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-hero\\",\\"notes\\":\\"hero\\"}},{\\"sectionId\\":\\"proof\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-proof\\",\\"notes\\":\\"proof\\"}}]}}\\n2) Texto da landing aprovado (JSON):\\n{}\\n3) Preset de design aprovado (JSON):\\n{\\"landingPageDesignPreset\\":{\\"sectionPresets\\":[{\\"sectionId\\":\\"hero\\",\\"surfaceStyle\\":\\"band\\",\\"contrastMode\\":\\"high\\"},{\\"sectionId\\":\\"proof\\",\\"surfaceStyle\\":\\"solid\\",\\"contrastMode\\":\\"normal\\"}]}}"}
                  ]
                }
                """;

        ExperimentPipelineJobCompletionPayload payload = client.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(), 38L, "landing-page-html", "gpt-5.1-codex", "prompt", requestBody, Instant.now()));

        Map<String, Object> htmlContent = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> html = (Map<String, Object>) htmlContent.get("landingPageHtml");
        String htmlDocument = String.valueOf(html.get("htmlDocument"));
        assertThat(htmlDocument).contains("data-section-id='hero' data-surface-token=\"surface-hero\" data-surface-style=\"band\" data-surface-contrast=\"high\"");
        assertThat(htmlDocument).contains("data-section-id='proof' data-surface-token=\"surface-proof\" data-surface-style=\"solid\" data-surface-contrast=\"normal\"");
    }

    @Test
    void failsFastWhenLandingHtmlCannotReproduceWireframeSurfaceSpec() {
        String htmlText = """
                {
                  "landingPageHtml": {
                    "htmlDocument": "<!doctype html><html><body><section data-section-id='hero'></section></body></html>",
                    "summary": "ok",
                    "formSpec": {"formId": "lead-capture-primary"},
                    "imagePlacementContract": {"requiredDataAttributes": []},
                    "consistencyChecks": []
                  }
                }
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), htmlText)),
                MAPPER,
                "test-key",
                "http://openai");

        String requestBody = """
                {
                  "model":"gpt-5.2",
                  "input":[
                    {"role":"user","content":"Prompt\\nWireframe da landing:\\n{\\"landingPageWireframe\\":{\\"sectionOrder\\":[{\\"sectionId\\":\\"hero\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-hero\\",\\"style\\":\\"band\\",\\"contrastMode\\":\\"high\\"}},{\\"sectionId\\":\\"cta\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-cta\\",\\"style\\":\\"solid\\",\\"contrastMode\\":\\"normal\\"}}]}}\\nPlanejamento de imagens da landing:\\n{}"}
                  ]
                }
                """;

        Throwable thrown = catchThrowable(() -> client.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(), 36L, "landing-page-html", "gpt-5.1-codex", "prompt", requestBody, Instant.now())));

        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Quebra de contrato: LANDING_PAGE_HTML divergente de landing-page-wireframe.sectionOrder.surfaceSpec.");
    }

    @Test
    void failsFastWhenLandingHtmlCannotReproduceWireframeSurfaceSpecUsingPromptV2Marker() {
        String htmlText = """
                {
                  "landingPageHtml": {
                    "htmlDocument": "<!doctype html><html><body><section data-section-id='proof'></section></body></html>",
                    "summary": "ok",
                    "formSpec": {"formId": "lead-capture-primary"},
                    "imagePlacementContract": {"requiredDataAttributes": []},
                    "consistencyChecks": []
                  }
                }
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), htmlText)),
                MAPPER,
                "test-key",
                "http://openai");

        String requestBody = """
                {
                  "model":"gpt-5.2",
                  "input":[
                    {"role":"user","content":"Prompt v2\\n1) Wireframe aprovado (JSON):\\n{\\"landingPageWireframe\\":{\\"sectionOrder\\":[{\\"sectionId\\":\\"hero-proof-form\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-hero\\",\\"style\\":\\"band\\",\\"contrastMode\\":\\"normal\\"}},{\\"sectionId\\":\\"proof\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-proof\\",\\"style\\":\\"solid\\",\\"contrastMode\\":\\"normal\\"}}]}}\\n2) Copy aprovada (JSON):\\n{}"}
                  ]
                }
                """;

        Throwable thrown = catchThrowable(() -> client.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(), 37L, "landing-page-html", "gpt-5.1-codex", "prompt", requestBody, Instant.now())));

        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Quebra de contrato: LANDING_PAGE_HTML divergente de landing-page-wireframe.sectionOrder.surfaceSpec.");
    }

    @Test
    void acceptsLandingHtmlWhenImagePlanningBindingsMatchExactly() throws Exception {
        String htmlText = """
                {
                  "landingPageHtml": {
                    "htmlDocument": "<!doctype html><html><body><section data-section-id='hero' data-surface-token='surface-hero' data-surface-style='band' data-surface-contrast='high'><img data-image-section-id='hero' data-image-binding-key='hero-main' alt='Hero'></section><section data-section-id='proof' data-surface-token='surface-proof' data-surface-style='solid' data-surface-contrast='normal'><img data-image-section-id='proof' data-image-binding-key='proof-packshot' alt='Proof'></section></body></html>",
                    "summary": "ok",
                    "formSpec": {"formId": "lead-capture-primary"},
                    "imagePlacementContract": {"requiredDataAttributes": []},
                    "consistencyChecks": []
                  }
                }
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), htmlText)),
                MAPPER,
                "test-key",
                "http://openai");

        String requestBody = """
                {
                  "model":"gpt-5.2",
                  "input":[
                    {"role":"user","content":"Prompt\\nWireframe da landing:\\n{\\"landingPageWireframe\\":{\\"sectionOrder\\":[{\\"sectionId\\":\\"hero\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-hero\\",\\"style\\":\\"band\\",\\"contrastMode\\":\\"high\\"}},{\\"sectionId\\":\\"proof\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-proof\\",\\"style\\":\\"solid\\",\\"contrastMode\\":\\"normal\\"}}]}}\\nPlanejamento de imagens da landing:\\n{\\"landingPageImagePlanning\\":{\\"images\\":[{\\"sectionId\\":\\"hero\\",\\"imageBindingKey\\":\\"hero-main\\"},{\\"sectionId\\":\\"proof\\",\\"imageBindingKey\\":\\"proof-packshot\\"}]}}"}
                  ]
                }
                """;

        ExperimentPipelineJobCompletionPayload payload = client.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(), 39L, "landing-page-html", "gpt-5.1-codex", "prompt", requestBody, Instant.now()));

        Map<String, Object> htmlContent = MAPPER.readValue(payload.responseContent(), new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        Map<String, Object> html = (Map<String, Object>) htmlContent.get("landingPageHtml");
        String htmlDocument = String.valueOf(html.get("htmlDocument"));
        assertThat(htmlDocument).contains("data-image-section-id='hero' data-image-binding-key='hero-main'");
        assertThat(htmlDocument).contains("data-image-section-id='proof' data-image-binding-key='proof-packshot'");
    }

    @Test
    void failsFastWhenLandingHtmlCannotReproduceImagePlanningBinding() {
        String htmlText = """
                {
                  "landingPageHtml": {
                    "htmlDocument": "<!doctype html><html><body><section data-section-id='hero' data-surface-token='surface-hero' data-surface-style='band' data-surface-contrast='high'><img data-image-section-id='hero' data-image-binding-key='hero-main' alt='Hero'></section><section data-section-id='proof' data-surface-token='surface-proof' data-surface-style='solid' data-surface-contrast='normal'><img data-image-section-id='proof' data-image-binding-key='proof-wrong' alt='Proof'></section></body></html>",
                    "summary": "ok",
                    "formSpec": {"formId": "lead-capture-primary"},
                    "imagePlacementContract": {"requiredDataAttributes": []},
                    "consistencyChecks": []
                  }
                }
                """;

        ExperimentPipelineOpenAiClient client = new ExperimentPipelineOpenAiClient(
                WebClient.builder().exchangeFunction(capturePayloadExchange(new AtomicReference<>(), htmlText)),
                MAPPER,
                "test-key",
                "http://openai");

        String requestBody = """
                {
                  "model":"gpt-5.2",
                  "input":[
                    {"role":"user","content":"Prompt v2\\n1) Wireframe aprovado (JSON):\\n{\\"landingPageWireframe\\":{\\"sectionOrder\\":[{\\"sectionId\\":\\"hero\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-hero\\",\\"style\\":\\"band\\",\\"contrastMode\\":\\"high\\"}},{\\"sectionId\\":\\"proof\\",\\"surfaceSpec\\":{\\"surfaceToken\\":\\"surface-proof\\",\\"style\\":\\"solid\\",\\"contrastMode\\":\\"normal\\"}}]}}\\n2) Copy aprovada (JSON):\\n{}\\n3) Preset de design aprovado (JSON):\\n{}\\n4) Planejamento de imagens aprovado (JSON):\\n{\\"landingPageImagePlanning\\":{\\"images\\":[{\\"sectionId\\":\\"hero\\",\\"imageBindingKey\\":\\"hero-main\\"},{\\"sectionId\\":\\"proof\\",\\"imageBindingKey\\":\\"proof-packshot\\"}]}}"}
                  ]
                }
                """;

        Throwable thrown = catchThrowable(() -> client.generate(new ExperimentPipelineJobDto(
                UUID.randomUUID(), 40L, "landing-page-html", "gpt-5.1-codex", "prompt", requestBody, Instant.now())));

        assertThat(thrown).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Quebra de contrato: LANDING_PAGE_HTML divergente de landing-page-image-planning.images[].sectionId/imageBindingKey.");
    }

    private ExchangeFunction capturePayloadExchange(AtomicReference<Map<String, Object>> payloadRef) {
        return capturePayloadExchange(payloadRef, "{\"content\":\"ok\"}");
    }

    @SuppressWarnings("unchecked")
    private String readCheckStatus(Map<String, Object> artifact, String checkName) {
        Object checksNode = artifact.get("consistencyChecks");
        if (!(checksNode instanceof List<?> checks)) {
            return "";
        }
        for (Object check : checks) {
            if (check instanceof Map<?, ?> map && checkName.equals(String.valueOf(map.get("check")))) {
                return String.valueOf(map.get("status"));
            }
        }
        return "";
    }

    private ExchangeFunction capturePayloadExchange(AtomicReference<Map<String, Object>> payloadRef,
                                                    String outputText) {
        return request ->
                readBodyAsString(request.body())
                        .flatMap(body -> {
                            try {
                                payloadRef.set(MAPPER.readValue(body, new TypeReference<>() {}));
                            } catch (Exception ex) {
                                return Mono.error(ex);
                            }
                            String responseBody;
                            try {
                                responseBody = MAPPER.writeValueAsString(Map.of(
                                        "id", "resp_test",
                                        "status", "completed",
                                        "output", List.of(Map.of(
                                                "type", "message",
                                                "content", List.of(Map.of(
                                                        "type", "output_text",
                                                        "text", outputText
                                                ))
                                        )),
                                        "usage", Map.of("input_tokens", 100, "output_tokens", 20, "total_tokens", 120)
                                ));
                            } catch (Exception ex) {
                                return Mono.error(ex);
                            }
                            ClientResponse response = ClientResponse.create(HttpStatus.OK)
                                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                                    .body(responseBody)
                                    .build();
                            return Mono.just(response);
                        });
    }

    @SuppressWarnings("unchecked")
    private <T extends org.springframework.http.ReactiveHttpOutputMessage> Mono<String> readBodyAsString(BodyInserter<?, ? super T> bodyInserter) {
        MockClientHttpRequest mockRequest = new MockClientHttpRequest(org.springframework.http.HttpMethod.POST, "http://localhost");
        BodyInserter.Context context = new BodyInserter.Context() {
            @Override
            public java.util.List<HttpMessageWriter<?>> messageWriters() {
                return ExchangeStrategies.withDefaults().messageWriters();
            }

            @Override
            public java.util.Optional<ServerHttpRequest> serverRequest() {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Map<String, Object> hints() {
                return java.util.Collections.emptyMap();
            }
        };

        return ((BodyInserter<Object, T>) bodyInserter)
                .insert((T) mockRequest, context)
                .then(Mono.defer(() ->
                        mockRequest.getBodyAsString().defaultIfEmpty("")));
    }
}
