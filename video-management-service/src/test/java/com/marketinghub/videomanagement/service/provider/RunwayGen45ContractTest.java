package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.videomanagement.client.BackendVideoClient;
import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import com.marketinghub.videomanagement.service.VideoJobObservabilityService;
import com.marketinghub.videomanagement.service.VideoProviderPreflightPoller;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: reproduzir os filtros reais do Gen-4.5 entre pending e callback, sem geração. */
class RunwayGen45ContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    /** Confere os filtros de campo e tamanho, isoladamente, da fila real até o callback. */
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void shouldCompletePreflightThroughRealPollerWithoutUnsupportedInputs(boolean enforceInputSupport)
            throws Exception {
        try (MockWebServer backend = new MockWebServer(); MockWebServer runway = new MockWebServer()) {
            backend.start();
            runway.start();
            backend.enqueue(json("[" + resource("gen45-pending.json") + "]"));
            backend.enqueue(new MockResponse().setResponseCode(204));
            runway.setDispatcher(new Dispatcher() {
                /** Simula filtros comprovados na API oficial e recusa qualquer request faturável. */
                @Override
                public MockResponse dispatch(RecordedRequest request) {
                    try {
                        if ("/v1/organization".equals(request.getPath())) {
                            return json(resource("gen45-organization.json"));
                        }
                        if (!"/v1/generate/video".equals(request.getPath())) {
                            return new MockResponse().setResponseCode(404);
                        }
                        JsonNode body = mapper.readTree(request.getBody().clone().readUtf8());
                        if (!body.path("dryRun").asBoolean()) {
                            return new MockResponse().setResponseCode(403);
                        }
                        if (enforceInputSupport && body.path("input").has("negativePrompt")) {
                            return json(resource("gen45-input-support-error.json")).setResponseCode(400);
                        }
                        if (body.path("input").path("promptText").asText().length() > 1000) {
                            return json(resource("gen45-prompt-length-error.json")).setResponseCode(400);
                        }
                        ObjectNode response = (ObjectNode) mapper.readTree(resource("gen45-dry-run.json"));
                        int seconds = body.path("input").path("duration").asInt();
                        ((ObjectNode) response.path("routing").path("resolvedInput")).put("duration", seconds);
                        ((ObjectNode) response.path("routing").path("estimatedCost")).put("credits", seconds * 12);
                        return json(response.toString());
                    } catch (IOException ex) {
                        throw new AssertionError("Fixture local do contrato Gen-4.5 inválida", ex);
                    }
                }
            });
            VideoManagementProperties properties = new VideoManagementProperties();
            properties.setBackendBaseUrl(backend.url("/").uri());
            properties.getJobs().setPollingEnabled(true);
            properties.getProviders().getRunway().setEnabled(true);
            properties.getProviders().getRunway().setBaseUrl(runway.url("/").uri());
            properties.getProviders().getRunway().setApiKey("runway-local-test");
            properties.getProviders().getRunway().setFinalRouterConfigId("qa-gen45");
            BackendVideoClient client = new BackendVideoClient(
                    WebClient.builder(), properties, mock(VideoJobObservabilityService.class));
            RunwayProviderPreflightService provider = new RunwayProviderPreflightService(
                    properties, mapper, new RunwayRouterRequestFactory(properties),
                    new RunwayProductUgcRequestFactory(), mock(RunwayReferenceImageInspector.class),
                    WebClient.builder());

            new VideoProviderPreflightPoller(properties, client, provider).pollPreflight();

            assertThat(backend.takeRequest(2, TimeUnit.SECONDS).getPath())
                    .isEqualTo("/api/internal/sales-videos/autonomy/v1/provider-preflight/pending");
            RecordedRequest callback = backend.takeRequest(2, TimeUnit.SECONDS);
            assertThat(callback).isNotNull();
            assertThat(callback.getPath())
                    .isEqualTo("/api/internal/sales-videos/autonomy/v1/cycles/91014/provider-preflight-result");
            JsonNode result = mapper.readTree(callback.getBody().readUtf8());
            assertThat(result.path("status").asText()).isEqualTo("READY");
            assertThat(result.path("failureCode").isNull()).isTrue();
            assertThat(result.path("estimatedCredits").asInt()).isEqualTo(180);
            JsonNode quotas = mapper.readTree(result.path("quotaSnapshotJson").asText());
            assertThat(quotas.isObject()).isTrue();
            assertThat(quotas.path("models")).hasSize(1);
            assertThat(quotas.path("models").get(0).path("requestedGenerations").asInt()).isEqualTo(2);
            assertThat(result.path("payloadSha256").asText()).hasSize(64);
            assertThat(result.path("routerConfigId").asText()).isEqualTo("qa-gen45");
            JsonNode frozen = mapper.readTree(result.path("executionRequestsJson").asText());
            assertThat(frozen).hasSize(2);
            assertThat(backend.getRequestCount()).isEqualTo(2);
            assertThat(runway.getRequestCount()).isEqualTo(3);
            assertThat(runway.takeRequest().getMethod()).isEqualTo("GET");
            for (int index = 0; index < 2; index++) {
                JsonNode request = mapper.readTree(runway.takeRequest().getBody().readUtf8());
                assertThat(request.path("dryRun").asBoolean()).isTrue();
                ((ObjectNode) request).remove("dryRun");
                assertThat(request).isEqualTo(frozen.get(index));
                assertThat(request.path("input").has("negativePrompt")).isFalse();
                String prompt = request.path("input").path("promptText").asText();
                assertThat(prompt.length()).isLessThanOrEqualTo(1000);
                assertThat(prompt).contains("steady, sharp", "post-production", "Participante adulta fictícia");
            }
        }
    }

    /** Recusa direção visual ainda longa sem cortar a cena, a continuidade ou os limites de segurança. */
    @Test
    void shouldRejectOversizedVisualDirectionWithoutSilentTruncation() throws Exception {
        ObjectNode context = (ObjectNode) mapper.readTree(resource("gen45-pending.json"));
        context.put("characterBible", "Característica visual indispensável. ".repeat(100));
        ProviderPreflightJob job = mapper.treeToValue(context, ProviderPreflightJob.class);

        assertThatThrownBy(() -> new RunwayRouterRequestFactory(new VideoManagementProperties()).build(job))
                .isInstanceOfSatisfying(VideoProviderException.class, error ->
                        assertThat(error.getCode()).isEqualTo("PROVIDER_PROMPT_TOO_LONG"))
                .hasMessageContaining("nenhuma regra foi truncada");
    }

    /** Carrega recibos oficiais sanitizados e contexto sintético sem IDs ou métricas produtivos. */
    private String resource(String name) throws IOException {
        try (var input = getClass().getResourceAsStream("/runway/gen45/" + name)) {
            if (input == null) throw new IOException("Fixture ausente: " + name);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Retorna JSON exclusivamente pelo servidor local da suíte. */
    private MockResponse json(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }
}
