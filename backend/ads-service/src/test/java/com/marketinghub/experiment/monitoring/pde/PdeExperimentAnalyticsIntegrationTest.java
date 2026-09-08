package com.marketinghub.experiment.monitoring.pde;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.marketinghub.experiment.Experiment;
import com.marketinghub.experiment.ExperimentType;
import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.experiment.monitoring.PostDeployMonitorService;
import com.marketinghub.experiment.monitoring.dto.PostDeployMonitorResponseDto;
import com.marketinghub.experiment.video.service.ExperimentVideoPerformanceDashboardService;
import com.marketinghub.facebookads.playbook.service.ExperimentFacebookApiLogService;
import com.marketinghub.growthoperator.controller.GrowthOperatorController;
import com.marketinghub.growthoperator.service.GrowthOperatorService;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.pde.service.PdeProductionSlotService;
import com.marketinghub.planning.CommercialPlan;
import com.marketinghub.planning.service.CommercialPlanService;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jdbc.experiment.ExperimentPdeAnalyticsRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentCampaignMetricRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Reproduz a leitura SQL, API e MCP de Hermes com métricas sintéticas isoladas de produção. */
public class PdeExperimentAnalyticsIntegrationTest {
  private JdbcTemplate jdbc;
  private ExperimentPdeAnalyticsRepository repository;
  private PdeExperimentAnalyticsReader reader;
  private Experiment experiment;
  private ExperimentRepository experiments;
  private PdeProductionSlotRepository slots;
  private GrowthOperatorService growth;
  private MockMvc mvc;
  private final ObjectMapper json =
      new ObjectMapper()
          .findAndRegisterModules()
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  /** Cria base efêmera; a mesma suíte aceita MySQL 5.7 real pela variável exclusiva de teste. */
  @BeforeEach
  void setUp() {
    String url = System.getenv("HERMES_TEST_JDBC_URL");
    boolean h2 = url == null;
    if (!h2
        && !url.matches(
            "jdbc:mysql://(sandbox-docker|127\\.0\\.0\\.1|localhost):[0-9]+/hermes_test(?:\\?.*)?"))
      throw new IllegalArgumentException("Use somente o banco hermes_test da sandbox isolada");
    jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                h2
                    ? "jdbc:h2:mem:hermes_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1"
                    : url,
                h2 ? "sa" : "root",
                h2 ? "" : "local-hermes-test"));
    if (h2) {
      jdbc.execute(
          "CREATE ALIAS SUBSTRING_INDEX FOR 'com.marketinghub.experiment.monitoring.pde.PdeExperimentAnalyticsIntegrationTest.substringIndex'");
      jdbc.execute(
          "CREATE ALIAS JSON_EXTRACT FOR 'com.marketinghub.experiment.monitoring.pde.PdeExperimentAnalyticsIntegrationTest.jsonExtract'");
      jdbc.execute(
          "CREATE ALIAS JSON_UNQUOTE FOR 'com.marketinghub.experiment.monitoring.pde.PdeExperimentAnalyticsIntegrationTest.jsonUnquote'");
    }
    for (String table :
        List.of(
            "pde_funnel_event",
            "facebook_ads_ad_tracking_utm",
            "facebook_ads_ad",
            "facebook_ads_ad_set",
            "facebook_ads_campaign")) jdbc.execute("DROP TABLE IF EXISTS " + table);
    jdbc.execute(
        "CREATE TABLE facebook_ads_campaign(id VARCHAR(50),external_id VARCHAR(50),experiment_id BIGINT)");
    jdbc.execute(
        "CREATE TABLE facebook_ads_ad_set(id VARCHAR(50),external_id VARCHAR(50),campaign_id VARCHAR(50))");
    jdbc.execute(
        "CREATE TABLE facebook_ads_ad(id VARCHAR(50),external_id VARCHAR(50),adset_id VARCHAR(50))");
    jdbc.execute(
        "CREATE TABLE facebook_ads_ad_tracking_utm(ad_id VARCHAR(50),utm_campaign VARCHAR(100),utm_content VARCHAR(100))");
    jdbc.execute(
        """
      CREATE TABLE pde_funnel_event(id BIGINT PRIMARY KEY AUTO_INCREMENT,event_id VARCHAR(64) UNIQUE,
      product_slug VARCHAR(191),experience_version VARCHAR(120),event_type VARCHAR(80),traffic_quality VARCHAR(30),
      session_id VARCHAR(100),visitor_id VARCHAR(100),utm_source VARCHAR(100),utm_medium VARCHAR(100),utm_campaign VARCHAR(100),utm_content VARCHAR(100),
      visible_ms BIGINT,occurred_at DATETIME,screen_width INT,screen_height INT,device_type VARCHAR(20),action_name VARCHAR(100),section_id VARCHAR(100),metadata_json TEXT)
      """);
    jdbc.update("INSERT INTO facebook_ads_campaign VALUES ('campaign-91','120251556536430326',91)");
    repository = new ExperimentPdeAnalyticsRepository(jdbc);
    slots = mock(PdeProductionSlotRepository.class);
    when(slots.findFirstByDomain("v7.example.test"))
        .thenReturn(
            Optional.of(
                PdeProductionSlot.builder()
                    .productSlug("musa")
                    .experienceVersion("musa-v7")
                    .domain("v7.example.test")
                    .build()));
    experiment =
        Experiment.builder()
            .id(91L)
            .product(Product.builder().id(4L).slug("musa").build())
            .experimentType(ExperimentType.PDE_MEMBERSHIP_SUBSCRIPTION_FUNNEL)
            .followUpActionUrl("https://v7.example.test")
            .build();
    reader = new PdeExperimentAnalyticsReader(slots, repository);
    experiments = mock(ExperimentRepository.class);
    when(experiments.findById(91L)).thenReturn(Optional.of(experiment));
    ExperimentFunnelService funnel =
        new ExperimentFunnelService(
            experiments, null, null, null, jdbc, null, null, null, slots, reader);
    CommercialPlanService plans = mock(CommercialPlanService.class);
    when(plans.getPlan(3L))
        .thenReturn(CommercialPlan.builder().id(3L).experiment(experiment).build());
    growth =
        new GrowthOperatorService(
            null,
            null,
            plans,
            null,
            funnel,
            null,
            mock(ExperimentVideoPerformanceDashboardService.class),
            null,
            json);
    mvc =
        MockMvcBuilders.standaloneSetup(new GrowthOperatorController(growth))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(json))
            .build();
  }

  /** Confirma contagens completas, escopo, preenchimento e igualdade entre o plano e o BPM. */
  @Test
  void readsActualCountsAcrossControllerPlanAndPdeMonitor() throws Exception {
    seedVega();
    var response = readApi();
    assertThat(response.path("available").asBoolean()).isTrue();
    assertThat(response.at("/pdeAnalytics/totalEvents").asLong()).isEqualTo(86);
    assertThat(response.at("/pdeAnalytics/sessions").asLong()).isEqualTo(4);
    assertThat(response.at("/pdeAnalytics/pageViews").asLong()).isEqualTo(4);
    assertThat(response.at("/pdeAnalytics/includedEvents").asLong()).isEqualTo(86);
    assertThat(response.at("/pdeAnalytics/eventMetrics").toString()).contains("FIELD_FILLED");
    assertThat(response.at("/pdeAnalytics/loginCompleted").asLong()).isZero();
    assertThat(response.at("/pdeAnalytics/detailedEvents/0/trafficQuality").asText())
        .isEqualTo("HUMAN");
    assertThat(response.at("/landingAnalytics/reason").asText()).isEqualTo("NOT_APPLICABLE_TO_PDE");
    var plan = json.valueToTree(growth.sessionIntelligence(3L, 2000));
    for (String key :
        List.of("totalEvents", "sessions", "eventMetrics", "experienceVersion", "detailedEvents"))
      assertThat(plan.at("/pdeAnalytics/" + key).toString())
          .isEqualTo(response.at("/pdeAnalytics/" + key).toString());
    String content = response.toString();
    assertThat(content)
        .doesNotContain(
            "visitor-secret",
            "session-secret",
            "qa-secret",
            "other-campaign",
            "other-product",
            "musa-v6",
            "cliente@example.test");
    PostDeployMonitorResponseDto monitor = monitor();
    assertThat(monitor.productSlug()).isEqualTo("musa");
    assertThat(monitor.pde().totalEvents()).isEqualTo(86);
    assertThat(monitor.pde().fieldFilled()).isEqualTo(1);
    assertThat(monitor.pde().pageViews()).isEqualTo(4);
    assertThat(monitor.pde().sessions()).isEqualTo(4);
    assertThat(monitor.pde().lastEventAt()).isEqualTo(Instant.parse("2026-09-07T19:18:32Z"));
    String output = System.getenv("HERMES_TEST_EVIDENCE_DIR");
    if (output != null) {
      Files.createDirectories(Path.of(output));
      Files.writeString(Path.of(output, "session-intelligence.json"), content);
      Files.writeString(Path.of(output, "monitor.json"), json.writeValueAsString(monitor));
    }
  }

  /** Retorna 404 para experimento inexistente, sem convertê-lo em analytics vazios. */
  @Test
  void rejectsUnknownExperiment() throws Exception {
    var response =
        mvc.perform(get("/api/growth-operator/v1/internal/experiments/999/session-intelligence"))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(404);
  }

  /** Exerce o MCP real como processo filho contra controller e repositório reais em HTTP local. */
  @Test
  void realMcpReadsTheCanonicalEndpoint() throws Exception {
    seedVega();
    List<String> paths = Collections.synchronizedList(new ArrayList<>());
    var server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          paths.add(exchange.getRequestURI().toString());
          byte[] body;
          try {
            body =
                (exchange.getRequestURI().getPath().startsWith("/api/internal/agent-memory")
                        ? "[]"
                        : mvc.perform(get(exchange.getRequestURI()))
                            .andReturn()
                            .getResponse()
                            .getContentAsString())
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
          } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                .error("Falha na API local de homologação Hermes", ex);
            throw new IOException(ex);
          }
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    Path entry =
        Path.of("../../growth-operator-worker/src/main/resources/mcp/marketing-hub-readonly.mjs")
            .toAbsolutePath()
            .normalize();
    ProcessBuilder builder = new ProcessBuilder("node", entry.toString());
    builder.environment().remove("MCP_COMMERCIAL_PLAN_ID");
    builder.environment().put("MCP_EXPERIMENT_ID", "91");
    builder
        .environment()
        .put("MCP_MARKETING_HUB_URL", "http://127.0.0.1:" + server.getAddress().getPort());
    builder.redirectError(ProcessBuilder.Redirect.DISCARD);
    Process process = builder.start();
    try {
      process
          .getOutputStream()
          .write(
              "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"consultar_sessoes\",\"arguments\":{}}}\n"
                  .getBytes());
      process.getOutputStream().flush();
      var task =
          java.util.concurrent.CompletableFuture.supplyAsync(
              () -> {
                try {
                  return new BufferedReader(new InputStreamReader(process.getInputStream()))
                      .readLine();
                } catch (IOException ex) {
                  throw new java.io.UncheckedIOException(ex);
                }
              });
      var envelope = json.readTree(task.get(15, java.util.concurrent.TimeUnit.SECONDS));
      var payload = json.readTree(envelope.at("/result/content/0/text").asText());
      assertThat(payload.at("/data/pdeAnalytics/totalEvents").asLong()).isEqualTo(86);
      assertThat(payload.at("/audit/source").asText())
          .contains("/internal/experiments/91/session-intelligence");
      assertThat(paths).noneMatch(path -> path.contains("funnel/analytics"));
    } finally {
      process.destroyForcibly();
      server.stop(0);
    }
  }

  /** Mantém ações, rolagem e ordem temporal mesmo quando um evento antigo chega depois. */
  @Test
  void preservesJourneyDetailsAndEventChronology() {
    add(
        "musa",
        "musa-v7",
        "120251556536430326",
        "HUMAN",
        "UI_CLICK",
        "session-sequence",
        "{\"screenName\":\"inicio\",\"maxScrollDepthPercent\":75,\"email\":\"cliente@example.test\"}");
    jdbc.update(
        "UPDATE pde_funnel_event SET action_name='cta-start',section_id='first-adjustment'");
    add("musa", "musa-v7", "120251556536430326", "HUMAN", "PAGE_VIEW", "session-sequence", "{}");
    jdbc.update(
        "UPDATE pde_funnel_event SET occurred_at='2026-09-07 16:00:00' WHERE event_type='PAGE_VIEW'");
    var summary = reader.read(experiment);
    var journey = summary.recentJourneys().getFirst();
    assertThat(journey.lastEventType()).isEqualTo("UI_CLICK");
    assertThat(journey.lastActionName()).isEqualTo("cta-start");
    assertThat(journey.maxScrollDepthPercent()).isEqualTo(75);
    assertThat(journey.screenNames()).containsExactly("inicio");
    assertThat(journey.sectionIds()).contains("first-adjustment");
    assertThat(journey.ctaClicked()).isTrue();
    var details = reader.details(experiment, summary, 10);
    assertThat(details.getFirst().attributes())
        .containsEntry("maxScrollDepthPercent", "75")
        .containsEntry("actionName", "cta-start");
    assertThat(details.toString()).doesNotContain("cliente@example.test");
  }

  /** A origem do anúncio permanece visível fora das vinte campanhas globais de maior volume. */
  @Test
  void readsAdContentRegardlessOfGlobalCampaignRanking() {
    jdbc.update(
        "INSERT INTO facebook_ads_ad_set VALUES ('set-91','external-set-91','campaign-91')");
    jdbc.update("INSERT INTO facebook_ads_ad VALUES ('ad-91','external-ad-91','set-91')");
    for (int i = 0; i < 25; i++) {
      for (int j = 0; j < 2; j++) {
        add(
            "musa",
            "musa-v7",
            "other-campaign-" + i,
            "HUMAN",
            "PED_ENTRY",
            "other-session-" + i + "-" + j,
            "{}");
      }
    }
    add("musa", "musa-v7", "custom-source", "HUMAN", "PED_ENTRY", "session-ad", "{}");
    jdbc.update(
        "UPDATE pde_funnel_event SET utm_content='external-ad-91' WHERE session_id='session-ad'");
    var summary = reader.read(experiment);
    assertThat(summary.totalEvents()).isEqualTo(1);
    assertThat(summary.sessions()).isEqualTo(1);
    assertThat(summary.trafficSources()).hasSize(1);
  }

  /** Retorna zero comprovado e limita o detalhe sem reduzir a agregação. */
  @Test
  void preservesZeroAndTruncation() throws Exception {
    assertThat(readApi().at("/pdeAnalytics/totalEvents").asLong()).isZero();
    assertThat(readApi().path("available").asBoolean()).isTrue();
    seedVega();
    var limited = json.valueToTree(growth.experimentSessionIntelligence(91L, 2));
    assertThat(limited.at("/pdeAnalytics/totalEventsAvailable").asLong()).isEqualTo(86);
    assertThat(limited.at("/pdeAnalytics/includedEvents").asLong()).isEqualTo(2);
    assertThat(limited.at("/pdeAnalytics/truncated").asBoolean()).isTrue();
  }

  /** Não depende das vinte origens de maior volume para encontrar a campanha do experimento. */
  @Test
  void ignoresGlobalTrafficAndReadsDirectExplicitAttribution() {
    jdbc.update("DELETE FROM facebook_ads_campaign");
    add("musa", "musa-v7", null, "HUMAN", "PED_ENTRY", "session-direct", "{\"experimentId\":91}");
    add("musa", "musa-v7", null, "HUMAN", "PED_ENTRY", "session-other", "{\"experimentId\":90}");
    assertThat(reader.read(experiment).sessions()).isEqualTo(1);
    assertThat(reader.read(experiment).totalEvents()).isEqualTo(1);
  }

  /**
   * Uma referência explícita a outro experimento prevalece sobre UTM residual de visita anterior.
   */
  @Test
  void doesNotAttributeAnotherExperimentThroughStaleUtm() {
    add(
        "musa",
        "musa-v7",
        "120251556536430326",
        "HUMAN",
        "PED_ENTRY",
        "session-conflict",
        "{\"experimentId\":90}");
    assertThat(reader.read(experiment).totalEvents()).isZero();
    assertThat(reader.read(experiment).sessions()).isZero();
  }

  /** Falha de banco, versão ausente e produto incompatível não viram zeros ou dados globais. */
  @Test
  void rejectsMissingScopeAndUnavailableDatabase() throws Exception {
    when(slots.findFirstByDomain("v7.example.test")).thenReturn(Optional.empty());
    assertThat(readApi().path("available").asBoolean()).isFalse();
    when(slots.findFirstByDomain("v7.example.test"))
        .thenReturn(
            Optional.of(
                PdeProductionSlot.builder()
                    .productSlug("mira")
                    .experienceVersion("mira-v2")
                    .build()));
    assertThat(readApi().path("available").asBoolean()).isFalse();
    when(slots.findFirstByDomain("v7.example.test"))
        .thenReturn(
            Optional.of(
                PdeProductionSlot.builder()
                    .productSlug("musa")
                    .experienceVersion("musa-v7")
                    .build()));
    jdbc.execute("DROP TABLE pde_funnel_event");
    var unavailable = readApi();
    assertThat(unavailable.path("available").asBoolean()).isFalse();
    assertThat(unavailable.at("/pdeAnalytics/reason").asText()).isEqualTo("PDE_ANALYTICS_ERROR");
    assertThat(unavailable.at("/landingAnalytics/reason").asText())
        .isEqualTo("NOT_APPLICABLE_TO_PDE");
  }

  /** Executa a rota publicada do próprio módulo de Hermes. */
  private com.fasterxml.jackson.databind.JsonNode readApi() throws Exception {
    var response =
        mvc.perform(get("/api/growth-operator/v1/internal/experiments/91/session-intelligence"))
            .andReturn()
            .getResponse();
    assertThat(response.getStatus()).isEqualTo(200);
    return json.readTree(response.getContentAsString());
  }

  /** Monta o painel real com integrações de saúde/Meta simuladas e métricas SQL reais. */
  private PostDeployMonitorResponseDto monitor() {
    PdeProductionSlotService slotService = mock(PdeProductionSlotService.class);
    when(slotService.listProductionSlotsForProduct("musa")).thenReturn(List.of());
    ExperimentFacebookApiLogService logs = mock(ExperimentFacebookApiLogService.class);
    return new PostDeployMonitorService(
            experiments,
            mock(ExperimentCampaignMetricRepository.class),
            logs,
            mock(PdeAnalyticsClient.class),
            slotService,
            jdbc,
            reader)
        .summarize(91L, "produto-divergente-na-url");
  }

  /** Reproduz a cardinalidade do #91 sem copiar dados pessoais de visitantes reais. */
  private void seedVega() {
    for (int i = 0; i < 4; i++) {
      add(
          "musa",
          "musa-v7",
          "120251556536430326",
          "HUMAN",
          "PED_ENTRY",
          "session-secret-" + i,
          "{}");
      add(
          "musa",
          "musa-v7",
          "120251556536430326",
          "HUMAN",
          "PAGE_VIEW",
          "session-secret-" + i,
          "{}");
    }
    for (String type : List.of("FIELD_FILLED", "LOGIN_STARTED", "PAYWALL_VIEWED"))
      add("musa", "musa-v7", "120251556536430326", "HUMAN", type, "session-secret-0", "{}");
    for (int i = 0; i < 75; i++)
      add(
          "musa",
          "musa-v7",
          "120251556536430326",
          "HUMAN",
          "UI_CLICK",
          "session-secret-" + (i % 4),
          "{\"email\":\"cliente@example.test\"}");
    add("musa", "musa-v7", "other-campaign", "HUMAN", "PED_ENTRY", "other-campaign", "{}");
    add(
        "other-product",
        "musa-v7",
        "120251556536430326",
        "HUMAN",
        "PED_ENTRY",
        "other-product",
        "{}");
    add("musa", "musa-v6", "120251556536430326", "HUMAN", "PED_ENTRY", "other-version", "{}");
    for (String quality : List.of("INTERNAL_QA", "BOT_SUSPECTED", "PLATFORM_CRAWLER", "UNKNOWN"))
      add(
          "musa",
          "musa-v7",
          "120251556536430326",
          quality,
          "PED_ENTRY",
          "qa-secret-" + quality,
          "{}");
  }

  /** Insere somente eventos sintéticos no banco temporário desta homologação. */
  private void add(
      String product,
      String version,
      String campaign,
      String quality,
      String type,
      String session,
      String metadata) {
    jdbc.update(
        "INSERT INTO pde_funnel_event(event_id,product_slug,experience_version,utm_campaign,traffic_quality,event_type,session_id,visitor_id,occurred_at,visible_ms,device_type,screen_width,screen_height,metadata_json) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        UUID.randomUUID().toString(),
        product,
        version,
        campaign,
        quality,
        type,
        session,
        "visitor-secret-" + session,
        "2026-09-07 16:18:32",
        100,
        "mobile",
        390,
        844,
        metadata);
  }

  /** Emula apenas a função JSON do MySQL nos testes unitários com H2. */
  public static String jsonExtract(String value, String path) throws Exception {
    if (value == null) return null;
    var node = new ObjectMapper().readTree(value).get(path.substring(2));
    return node == null ? null : node.toString();
  }

  /** Emula a extração textual de JSON quando o teste não usa MySQL real. */
  public static String jsonUnquote(String value) throws Exception {
    return value == null ? null : new ObjectMapper().readTree(value).asText();
  }

  /** Simula no H2 somente a extração do primeiro ID ordenado usada pelo SQL MySQL. */
  public static String substringIndex(String value, String delimiter, int count) {
    if (value == null) return null;
    int index = value.indexOf(delimiter);
    return index < 0 ? value : value.substring(0, index);
  }
}
