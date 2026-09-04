package com.marketinghub.videomanagement.service.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.videomanagement.client.dto.ProviderPreflightJob;
import com.marketinghub.videomanagement.client.payload.ProviderPreflightResultPayload;
import com.marketinghub.videomanagement.config.VideoManagementProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/** Responsabilidade: validar saldo, quota e dry run Runway sem geração cobrável. */
class RunwayProviderPreflightServiceTest {
    private MockWebServer server;
    private ObjectMapper objectMapper;

    /** Inicializa uma API Runway simulada e um serializador estável. */
    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        objectMapper = new ObjectMapper();
    }

    /** Encerra a API simulada após cada cenário. */
    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    /** Consulta organização e dry run, mas congela o payload futuro sem `dryRun`. */
    @Test
    void shouldProduceReadySnapshotWithoutCreatingVideo() throws Exception {
        server.enqueue(json(organization(500, 1, 20)));
        server.enqueue(json(routing("gen4_turbo", 50)));
        RunwayProviderPreflightService service = service(properties());

        ProviderPreflightResultPayload result = service.execute(job());

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.officialBalanceCredits()).isEqualByComparingTo("500");
        assertThat(result.estimatedCredits()).isEqualByComparingTo("50");
        assertThat(result.maxMonthlyCreditSpend()).isEqualTo(10000L);
        assertThat(result.organizationSnapshotJson())
                .contains("creditBalance", "maxMonthlyCreditSpend", "models")
                .doesNotContain("internalOrganizationId", "billingEmail");
        assertThat(result.executionRequestsJson()).doesNotContain("dryRun");
        assertThat(result.selectedRoutesJson())
                .contains("\"manufacturer\":\"Runway\"")
                .contains("\"model\":\"gen4_turbo\"")
                .contains("\"aggregator\":\"Runway\"")
                .contains("\"accountKey\":\"RUNWAY_PRIMARY\"")
                .contains("\"batchRouteId\":\"RUNWAY_ROUTER:marketing-hub-campaign-final-v1\"");

        RecordedRequest organizationRequest = server.takeRequest();
        assertThat(organizationRequest.getMethod()).isEqualTo("GET");
        assertThat(organizationRequest.getPath()).isEqualTo("/v1/organization");
        assertThat(organizationRequest.getHeader("Authorization"))
                .isEqualTo("Bearer runway-test-key");
        RecordedRequest dryRun = server.takeRequest();
        assertThat(dryRun.getPath()).isEqualTo("/v1/generate/video");
        JsonNode body = objectMapper.readTree(dryRun.getBody().readUtf8());
        assertThat(body.path("dryRun").asBoolean()).isTrue();
        assertThat(body.path("configId").asText())
                .isEqualTo("marketing-hub-campaign-final-v1");
        assertThat(body.path("input").path("duration").asInt()).isEqualTo(10);
        assertThat(body.path("input").path("promptText").asText())
                .contains("AI-powered digital experience")
                .contains("steady, sharp")
                .contains("post-production");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    /** Preserva saldo e custo para Plutus orientar o bloqueio de quota sem geração paga. */
    @Test
    void shouldBlockExhaustedQuotaWithTheObtainedEvidence() {
        server.enqueue(json(organization(500, 20, 20)));
        server.enqueue(json(routing("gen4_turbo", 50)));

        ProviderPreflightResultPayload result = service(properties()).execute(job());

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.failureCode()).isEqualTo("PROVIDER_DAILY_QUOTA_EXCEEDED");
        assertThat(result.officialBalanceCredits()).isEqualByComparingTo("500");
        assertThat(result.estimatedCredits()).isEqualByComparingTo("50");
        assertThat(result.quotaSnapshotJson()).contains("remainingDailyGenerations");
        assertThat(result.executionRequestsJson()).doesNotContain("dryRun");
    }

    /** Classifica configuração ausente sem tentar geração direta ou esconder o erro HTTP. */
    @Test
    void shouldBlockMissingRouterConfigurationWithoutPaidFallback() {
        server.enqueue(json(organization(500, 1, 20)));
        server.enqueue(new MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"router_config_not_found\"}"));

        ProviderPreflightResultPayload result = service(properties()).execute(job());

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.failureCode()).isEqualTo("PROVIDER_ROUTER_CONFIG_MISSING");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    /** Bloqueia antes da rede quando a conta não possui credencial montada. */
    @Test
    void shouldBlockMissingCredentialBeforeCallingRunway() {
        VideoManagementProperties properties = properties();
        properties.getProviders().getRunway().setApiKey("");

        ProviderPreflightResultPayload result = service(properties).execute(job());

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.failureCode()).isEqualTo("PROVIDER_AUTH_ERROR");
        assertThat(server.getRequestCount()).isZero();
    }

    /** Recusa conta de outro agregador antes de ler segredo ou fazer chamada externa. */
    @Test
    void shouldRejectAccountFromAnotherAggregatorBeforeCallingRunway() {
        ProviderPreflightResultPayload result =
                service(properties()).execute(job("Atlas Cloud", "ATLAS_PRIMARY"));

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.failureCode()).isEqualTo("PROVIDER_ACCOUNT_UNSUPPORTED");
        assertThat(server.getRequestCount()).isZero();
    }

    /** Entrega a Plutus o teto inseguro sem criar tarefa faturável. */
    @Test
    void shouldBlockRouterPriceCeilingAboveCycleBudget() {
        server.enqueue(json(organization(500, 1, 20)));
        server.enqueue(json(routing("gen4_turbo", 50, 201)));

        ProviderPreflightResultPayload result = service(properties()).execute(job());

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.failureCode()).isEqualTo("PROVIDER_ROUTER_CEILING_UNSAFE");
        assertThat(result.failureDetail()).contains("tetos do Router");
        assertThat(result.executionRequestsJson()).doesNotContain("dryRun");
    }

    /** Recusa resposta que não confirma explicitamente a simulação sem cobrança. */
    @Test
    void shouldRejectRoutingResponseWithoutDryRunMarker() {
        server.enqueue(json(organization(500, 1, 20)));
        server.enqueue(json(routing("gen4_turbo", 50).replace("\"dryRun\": true,", "\"dryRun\": false,")));

        ProviderPreflightResultPayload result = service(properties()).execute(job());

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.failureCode()).isEqualTo("PROVIDER_PREFLIGHT_INVALID_RESPONSE");
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    /** Preflighta Product UGC por saldo e tarifa pinada sem chamar o endpoint faturável. */
    @Test
    void shouldPreflightProductUgcWithoutCallingTheRecipe() throws Exception {
        server.enqueue(json(organization(2020, 1, 20)));

        ProviderPreflightResultPayload result = service(properties()).execute(productUgcJob());

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.routerConfigId()).isEqualTo("product_ugc@2026-06");
        assertThat(result.officialBalanceCredits()).isEqualByComparingTo("2020");
        assertThat(result.estimatedCredits()).isEqualByComparingTo("648");
        assertThat(result.executionRequestsJson())
                .contains("\"version\":\"2026-06\"")
                .contains("\"ratio\":\"1080:1920\"")
                .contains("\"audio\":false")
                .doesNotContain("dryRun");
        assertThat(result.routingResponseJson())
                .contains("DETERMINISTIC_RATE_CARD")
                .contains("não publica contrato de dryRun")
                .contains("APOLLO_RUNWAY_PRODUCT_UGC_V1", "response-schema.json");
        assertThat(result.selectedRoutesJson())
                .contains("RUNWAY_PRODUCT_UGC:product_ugc@2026-06")
                .contains("\"priceCeilingCredits\":648")
                .contains("\"role\":\"CHARACTER_IMAGE\"")
                .contains("\"role\":\"PRODUCT_IMAGE\"")
                .contains("\"promptAndResponseContract\"")
                .contains("\"sha256\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"");
        assertThat(server.getRequestCount()).isEqualTo(1);
        assertThat(server.takeRequest().getPath()).isEqualTo("/v1/organization");
    }

    /** Bloqueia fallback HTML ou imagem inválida antes mesmo de consultar a conta Runway. */
    @Test
    void shouldBlockInvalidProductUgcReferenceBeforeProviderRequest() {
        RunwayReferenceImageInspector inspector = mock(RunwayReferenceImageInspector.class);
        when(inspector.inspectProductUgc(anyString(), anyString()))
                .thenThrow(
                        new VideoProviderException(
                                "PROVIDER_REFERENCE_INVALID",
                                "A referência do PDE não é uma imagem raster."));

        ProviderPreflightResultPayload result = service(properties(), inspector).execute(productUgcJob());

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.failureCode()).isEqualTo("PROVIDER_REFERENCE_INVALID");
        assertThat(server.getRequestCount()).isZero();
    }

    /** Bloqueia o gasto visual quando a voz natural necessária ao acabamento não está pronta. */
    @Test
    void shouldBlockProductUgcBeforeRunwayWhenPremiumVoiceIsUnavailable() {
        VideoManagementProperties properties = properties();
        properties.getProviders().getPostProduction().setOpenAiApiKey("");

        ProviderPreflightResultPayload result = service(properties).execute(productUgcJob());

        assertThat(result.status()).isEqualTo("BLOCKED");
        assertThat(result.failureCode()).isEqualTo("APOLLO_PREMIUM_AUDIO_UNAVAILABLE");
        assertThat(result.failureDetail()).contains("credencial de voz natural");
        assertThat(server.getRequestCount()).isZero();
    }

    /** Monta o serviço com as três responsabilidades concretas do adapter Runway. */
    private RunwayProviderPreflightService service(VideoManagementProperties properties) {
        return service(properties, readyReferenceInspector());
    }

    /** Monta o serviço com um inspetor explícito para cenários de integridade das referências. */
    private RunwayProviderPreflightService service(
            VideoManagementProperties properties, RunwayReferenceImageInspector inspector) {
        return new RunwayProviderPreflightService(
                properties,
                objectMapper,
                new RunwayRouterRequestFactory(properties),
                new RunwayProductUgcRequestFactory(),
                inspector,
                WebClient.builder());
    }

    /** Simula duas imagens raster públicas já inspecionadas sem acessar rede externa nos testes. */
    private RunwayReferenceImageInspector readyReferenceInspector() {
        RunwayReferenceImageInspector inspector = mock(RunwayReferenceImageInspector.class);
        when(inspector.inspectProductUgc(anyString(), anyString()))
                .thenReturn(
                        java.util.List.of(
                                new RunwayReferenceImageInspector.Evidence(
                                        "CHARACTER_IMAGE",
                                        "assets.example",
                                        "image/png",
                                        1000,
                                        1080,
                                        1920,
                                        "a".repeat(64)),
                                new RunwayReferenceImageInspector.Evidence(
                                        "PRODUCT_IMAGE",
                                        "assets.example",
                                        "image/png",
                                        900,
                                        1080,
                                        1920,
                                        "b".repeat(64))));
        when(inspector.audit(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(
                        invocation -> {
                            RunwayReferenceImageInspector.Evidence evidence = invocation.getArgument(0);
                            return java.util.Map.of(
                                    "role", evidence.role(),
                                    "sourceHost", evidence.sourceHost(),
                                    "contentType", evidence.contentType(),
                                    "contentLength", evidence.contentLength(),
                                    "width", evidence.width(),
                                    "height", evidence.height(),
                                    "sha256", evidence.sha256());
                        });
        return inspector;
    }

    /** Configura o adapter contra a API simulada sem qualquer segredo real. */
    private VideoManagementProperties properties() {
        VideoManagementProperties properties = new VideoManagementProperties();
        properties.getProviders().getRunway().setEnabled(true);
        properties.getProviders().getRunway().setBaseUrl(URI.create(server.url("/").toString()));
        properties.getProviders().getRunway().setApiKey("runway-test-key");
        properties.getProviders().getRunway().setPollInterval(Duration.ofMillis(1));
        properties.getProviders().getPostProduction().setEnabled(true);
        properties.getProviders().getPostProduction().setOpenAiTtsEnabled(true);
        properties.getProviders().getPostProduction().setOpenAiApiKey("openai-test-key");
        return properties;
    }

    /** Cria um ciclo final de campanha com um único clipe vertical. */
    private ProviderPreflightJob job() {
        return job("Runway", "RUNWAY_PRIMARY");
    }

    /** Cria um ciclo permitindo provar a segregação entre agregador e conta. */
    private ProviderPreflightJob job(String aggregatorName, String accountKey) {
        return new ProviderPreflightJob(
                31L,
                11L,
                aggregatorName,
                accountKey,
                "FINAL_CAMPAIGN",
                null,
                new BigDecimal("200"),
                10,
                10,
                1,
                "9:16",
                "720p",
                false,
                "Vega",
                "Converter interesse em diagnóstico",
                "Você se arruma, mas algo ainda não encaixa?",
                "Mostre a experiência digital MUSA e seu resultado prático.",
                "Espelho; experiência digital; decisão",
                "Mesma mulher adulta em todos os clipes",
                "Quarto claro com espelho",
                "Tela do PDE",
                "Editorial natural",
                "Preservar rosto, figurino, luz e direção",
                "Texto",
                "CTA",
                null,
                null,
                null,
                null,
                null,
                "Edição",
                "Qualidade",
                "Validar o gancho",
                "Retenção e CTA superiores");
    }

    /** Cria o contrato premium com duas referências e governança de direitos. */
    private ProviderPreflightJob productUgcJob() {
        return new ProviderPreflightJob(
                32L,
                91L,
                "Runway",
                "RUNWAY_PRIMARY",
                "FINAL_CAMPAIGN",
                "Provider escolhido no Estudio: Runway Product UGC Premium (RUNWAY_PRODUCT_UGC).",
                new BigDecimal("1000"),
                15,
                15,
                1,
                "1080:1920",
                "1080p",
                false,
                "Vega #91",
                "Gerar clique qualificado",
                "Ainda falta presença?",
                "MUSA transforma escolhas em plano com IA.",
                "Dúvida, mecanismo, alívio e CTA",
                "Mulher brasileira adulta",
                "Ambiente claro sem espelho",
                "Tela do diagnóstico MUSA",
                "UGC premium",
                "Uma tomada estável",
                "Você se arruma, mas falta presença? | Faça o diagnóstico gratuito.",
                "Faça o diagnóstico gratuito",
                "image",
                "https://assets.example/apresentadora.png",
                "https://assets.example/musa-pde.png",
                "consentimento-91",
                "direitos-91",
                "Sem texto nativo",
                "Sem tremor e com texto sincronizado",
                "Validar o criativo premium",
                "Retenção e CTA superiores");
    }

    /** Simula a resposta oficial da organização com tier e uso por modelo. */
    private String organization(int balance, int dailyUsed, int dailyLimit) {
        return """
                {
                  "creditBalance": %d,
                  "tier": {
                    "maxMonthlyCreditSpend": 10000,
                    "models": {
                      "gen4_turbo": {
                        "maxConcurrentGenerations": 2,
                        "maxDailyGenerations": %d
                      }
                    }
                  },
                  "usage": {
                    "models": {
                      "gen4_turbo": {"dailyGenerations": %d}
                    }
                  },
                  "internalOrganizationId": "must-not-be-persisted",
                  "billingEmail": "must-not-be-persisted@example.test"
                }
                """.formatted(balance, dailyLimit, dailyUsed);
    }

    /** Simula a decisão do Model Router sem criar uma task faturável. */
    private String routing(String model, int credits) {
        return routing(model, credits, 200);
    }

    /** Simula a decisão do Router com teto máximo por geração configurável. */
    private String routing(String model, int credits, int priceCeiling) {
        return """
                {
                  "dryRun": true,
                  "routing": {
                    "provider": "Runway",
                    "model": "%s",
                    "configId": "marketing-hub-campaign-final-v1",
                    "estimatedCost": {"credits": %d},
                    "resolvedSettings": {"optimizeFor": "quality", "priceCeiling": %d},
                    "resolvedInput": {"duration": 10, "aspectRatio": "9:16", "resolution": "720p"}
                  }
                }
                """.formatted(model, credits, priceCeiling);
    }

    /** Cria uma resposta JSON da API simulada. */
    private MockResponse json(String body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
