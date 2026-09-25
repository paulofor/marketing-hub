package com.marketinghub.facebookadsworker.resumption;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import okhttp3.mockwebserver.*;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

/** Homologa HTTP ponta a ponta com Meta e destino simulados, sem credenciais nem tráfego reais. */
class FacebookCampaignResumptionWorkerTest {
  private static final String REPLACEMENT_CAMPAIGN_ID = "120000000000000011";
  private static final String REPLACEMENT_AD_SET_ID = "120000000000000012";
  private static final String REPLACEMENT_AD_ID = "120000000000000013";
  private static final String SOURCE_AD_ID = "120000000000000003";
  private static final String SOURCE_CREATIVE_ID = "1399338238757778";
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(FacebookCampaignResumptionWorkerTest.class);
  private final ObjectMapper json = new ObjectMapper();
  private MockWebServer meta, backend, landing;
  private FacebookCampaignResumptionWorker worker;
  private ObjectNode task;
  private JsonNode result;
  private final List<JsonNode> writes = new CopyOnWriteArrayList<>();
  private final List<String> writePaths = new CopyOnWriteArrayList<>();
  private boolean rejectBudget,
      wrongReadback,
      wrongCampaignLifetimeBudget,
      wrongCampaignStopTime,
      emptyInsights,
      backendUnavailable,
      rejectSuccessCallback,
      successCallbackCommittedDespiteFailure,
      wrongReplacementReadback,
      sourcePauseDivergesAfterReplacementActivation;
  private boolean dailyMode;
  private boolean campaignCapUsable = true;
  private String campaignStatus = "PAUSED";
  private long campaignSpendCap;
  private long campaignDailyBudget;
  private long campaignLifetimeBudget;
  private long adSetLifetimeSpendCap;
  private long minimumCampaignSpendCap;
  private boolean adSetDailyBudgetCleared;
  private String finalEnd;
  private String replacementCampaignStatus = "PAUSED";
  private String replacementAdSetStatus = "PAUSED";
  private boolean replacementCampaignCreated;
  private boolean replacementAdSetCreated;
  private boolean replacementAdCreated;
  private long replacementLifetimeBudget;
  private int probes;

  /** Inicia três dependências HTTP descartáveis e uma autorização sintética. */
  @BeforeEach
  void setup() throws Exception {
    meta = new MockWebServer();
    backend = new MockWebServer();
    landing = new MockWebServer();
    meta.start();
    backend.start();
    landing.start();
    var startDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
    var endDate = startDate.plusDays(6);
    finalEnd =
        endDate
            .atTime(23, 59, 59)
            .atZone(ZoneId.of("America/Sao_Paulo"))
            .toInstant()
            .toString()
            .replace("Z", "+0000");
    task =
        json.createObjectNode()
            .put("id", 1)
            .put("experimentId", 91)
            .put("campaignId", "campaign")
            .put("adSetId", "adset")
            .put("totalLimit", 150)
            .put("dailyBudget", 20)
            .put("historicalSpend", 0)
            .put("startDate", startDate.toString())
            .put("endDate", endDate.toString())
            .put("leaseToken", "test-lease")
            .put("destinationUrl", landing.url("/").toString());
    landing.setDispatcher(
        new Dispatcher() {
          /** Confirma que todas as sondas estão segregadas do tráfego comercial. */
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            if (!request.getPath().contains("mh_audit="))
              return new MockResponse().setResponseCode(400);
            probes++;
            return new MockResponse()
                .addHeader("Content-Type", "text/html")
                .setBody("<html>" + "Teste funcional ".repeat(40) + "</html>");
          }
        });
    backend.setDispatcher(
        new Dispatcher() {
          /** Simula fila, reserva e persistência do callback oficial. */
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            try {
              if (request.getPath().endsWith("/pending"))
                return backendUnavailable
                    ? new MockResponse().setResponseCode(503)
                    : ok("[" + task + "]");
              if (request.getPath().endsWith("/claim")) return ok(task.toString());
              if ("GET".equals(request.getMethod()) && request.getPath().endsWith("/1"))
                return ok(
                    "{\"status\":\""
                        + (successCallbackCommittedDespiteFailure ? "COMPLETED" : "RUNNING")
                        + "\"}");
              if (request.getPath().endsWith("/result")) {
                result = json.readTree(request.getBody().readUtf8());
                if (rejectSuccessCallback && result.path("success").asBoolean())
                  return new MockResponse().setResponseCode(503);
                return ok("{}");
              }
              return new MockResponse().setResponseCode(404);
            } catch (Exception ex) {
              LOG.error("Falha na integração HTTP simulada: endpoint={}", request.getPath(), ex);
              throw new AssertionError(ex);
            }
          }
        });
    meta.setDispatcher(
        new Dispatcher() {
          /** Mantém estado Meta simulado para verificar ordem, moeda, limites e confirmação. */
          @Override
          public MockResponse dispatch(RecordedRequest request) {
            try {
              String path = request.getRequestUrl().encodedPath();
              if ("POST".equals(request.getMethod())) {
                JsonNode body = json.readTree(request.getBody().readUtf8());
                writes.add(body);
                writePaths.add(path);
                if (path.endsWith("/campaign")
                    && body.has("spend_cap")
                    && body.path("spend_cap").asLong() == 0L)
                  return new MockResponse()
                      .setResponseCode(400)
                      .setBody(
                          "{\"error\":{\"message\":\"Spend limit cannot be"
                              + " zero\",\"code\":100,\"error_subcode\":1885099}}");
                if (path.endsWith("/adset")
                    && body.has("daily_budget")
                    && body.path("daily_budget").asLong() == 0L)
                  return new MockResponse()
                      .setResponseCode(400)
                      .setBody(
                          "{\"error\":{\"message\":\"Budget too low\",\"code\":100,"
                              + "\"error_subcode\":1885272}}");
                if (body.has("lifetime_spend_cap")
                    && (!adSetDailyBudgetCleared || campaignDailyBudget <= 0L))
                  return new MockResponse()
                      .setResponseCode(400)
                      .setBody(
                          "{\"error\":{\"message\":\"spend limits and budget are mutually"
                              + " exclusive\",\"code\":100,\"error_subcode\":1885624}}");
                if ((body.has("lifetime_budget") || body.has("lifetime_spend_cap")) && rejectBudget)
                  return new MockResponse()
                      .setResponseCode(400)
                      .setBody(
                          "{\"error\":{\"message\":\"invalid"
                              + " budget\",\"code\":100,\"error_subcode\":2446307}}");
                if (path.endsWith("/act_123456/campaigns")) {
                  replacementCampaignCreated = true;
                  replacementCampaignStatus = body.path("status").asText();
                  return ok("{\"id\":\"" + REPLACEMENT_CAMPAIGN_ID + "\"}");
                }
                if (path.endsWith("/act_123456/adsets")) {
                  replacementAdSetCreated = true;
                  replacementLifetimeBudget = body.path("lifetime_budget").asLong();
                  replacementAdSetStatus = body.path("status").asText();
                  return ok("{\"id\":\"" + REPLACEMENT_AD_SET_ID + "\"}");
                }
                if (path.endsWith("/act_123456/ads")) {
                  replacementAdCreated = true;
                  return ok("{\"id\":\"" + REPLACEMENT_AD_ID + "\"}");
                }
                if (path.endsWith("/" + REPLACEMENT_CAMPAIGN_ID) && body.has("status")) {
                  replacementCampaignStatus = body.path("status").asText();
                  return ok("{\"success\":true}");
                }
                if (path.endsWith("/" + REPLACEMENT_AD_SET_ID) && body.has("status")) {
                  replacementAdSetStatus = body.path("status").asText();
                  return ok("{\"success\":true}");
                }
                if (path.endsWith("/" + REPLACEMENT_AD_ID) && body.has("status"))
                  return ok("{\"success\":true}");
                if (path.endsWith("/campaign") && body.has("status"))
                  campaignStatus = body.path("status").asText();
                if (path.endsWith("/campaign") && body.has("spend_cap"))
                  campaignSpendCap = body.path("spend_cap").asLong();
                if (path.endsWith("/campaign") && body.has("daily_budget")) {
                  campaignDailyBudget = body.path("daily_budget").asLong();
                  adSetDailyBudgetCleared = true;
                }
                if (path.endsWith("/campaign") && body.has("lifetime_budget")) {
                  campaignLifetimeBudget = body.path("lifetime_budget").asLong();
                  campaignDailyBudget = 0L;
                  adSetDailyBudgetCleared = true;
                }
                if (path.endsWith("/adset") && body.has("lifetime_spend_cap"))
                  adSetLifetimeSpendCap = body.path("lifetime_spend_cap").asLong();
                return ok("{\"success\":true}");
              }
              if (path.endsWith("/insights")) {
                assertThat(
                        json.readTree(request.getRequestUrl().queryParameter("time_range"))
                            .path("since")
                            .asText())
                    .isEqualTo("2026-09-07");
                return ok(emptyInsights ? "{\"data\":[]}" : "{\"data\":[{\"spend\":\"27.45\"}]}");
              }
              String fields = request.getRequestUrl().queryParameter("fields");
              if (path.endsWith("/act_123456/campaigns"))
                return replacementCampaignCreated
                    ? ok("{\"data\":[" + replacementCampaignJson() + "]}")
                    : ok("{\"data\":[]}");
              if (path.endsWith("/" + REPLACEMENT_CAMPAIGN_ID + "/adsets"))
                return replacementAdSetCreated
                    ? ok("{\"data\":[" + replacementAdSetJson() + "]}")
                    : ok("{\"data\":[]}");
              if (path.endsWith("/" + REPLACEMENT_AD_SET_ID + "/ads"))
                return replacementAdCreated
                    ? ok("{\"data\":[" + replacementAdJson() + "]}")
                    : ok("{\"data\":[]}");
              if (path.endsWith("/" + REPLACEMENT_CAMPAIGN_ID))
                return ok(replacementCampaignJson());
              if (path.endsWith("/" + REPLACEMENT_AD_SET_ID))
                return ok(replacementAdSetJson());
              if (path.endsWith("/" + REPLACEMENT_AD_ID))
                return ok(replacementAdJson());
              if ("currency,min_campaign_group_spend_cap".equals(fields))
                return ok(
                    "{\"currency\":\"BRL\",\"min_campaign_group_spend_cap\":\""
                        + minimumCampaignSpendCap
                        + "\"}");
              if ("start_time".equals(fields))
                return ok("{\"start_time\":\"2026-09-07T00:13:13-0300\"}");
              if (path.endsWith("/adset"))
                return dailyMode
                    ? ok(
                        "{\"id\":\"adset\",\"status\":\"ACTIVE\",\"daily_budget\":\""
                            + (adSetDailyBudgetCleared ? 0 : (wrongReadback ? 1000 : 2000))
                            + "\",\"lifetime_budget\":\"0\",\"lifetime_spend_cap\":\""
                            + adSetLifetimeSpendCap
                            + "\",\"end_time\":\""
                            + finalEnd
                            + "\"}")
                    : ok(
                        "{\"id\":\"adset\",\"status\":\"ACTIVE\",\"daily_budget\":\"0\",\"lifetime_budget\":\""
                            + (wrongReadback ? 10000 : 15000)
                            + "\",\"end_time\":\""
                            + finalEnd
                            + "\"}");
              if (fields.contains("adsets")) return ok(sourceCampaignJson());
              if (fields.contains("spend_cap,daily_budget,lifetime_budget"))
                return ok(
                    "{\"id\":\"campaign\",\"status\":\"PAUSED\",\"spend_cap\":\""
                        + campaignSpendCap
                        + "\",\"daily_budget\":\""
                        + campaignDailyBudget
                        + "\",\"lifetime_budget\":\""
                        + (wrongCampaignLifetimeBudget && campaignLifetimeBudget > 0
                            ? campaignLifetimeBudget - 100
                            : campaignLifetimeBudget)
                        + "\",\"stop_time\":\""
                        + (wrongCampaignStopTime
                            ? Instant.parse(finalEnd.replace("+0000", "Z")).minusSeconds(60)
                            : finalEnd)
                        + "\"}");
              if (sourcePauseDivergesAfterReplacementActivation
                  && path.endsWith("/campaign")
                  && "ACTIVE".equals(replacementCampaignStatus))
                return ok(
                    "{\"id\":\"campaign\",\"status\":\"ACTIVE\",\"effective_status\":\"ACTIVE\"}");
              return ok(
                  "{\"id\":\"campaign\",\"status\":\""
                      + campaignStatus
                      + "\",\"effective_status\":\""
                      + campaignStatus
                      + "\"}");
            } catch (Exception ex) {
              LOG.error("Falha na integração HTTP simulada: endpoint={}", request.getPath(), ex);
              throw new AssertionError(ex);
            }
          }
        });
    var config = mock(FacebookWorkerConfigurationClient.class);
    when(config.fetchConfiguration())
        .thenReturn(
            Optional.of(
                json.treeToValue(
                    json.readTree("{\"accessToken\":\"synthetic-test-only\"}"),
                    FacebookWorkerConfigurationClient.FacebookWorkerConfiguration.class)));
    worker =
        new FacebookCampaignResumptionWorker(
            WebClient.builder(),
            config,
            json,
            backend.url("/").toString().replaceAll("/$", ""),
            meta.url("/").toString().replaceAll("/$", ""),
            "v23.0");
  }

  /** Fecha todas as dependências efêmeras da homologação. */
  @AfterEach
  void cleanup() throws Exception {
    meta.close();
    backend.close();
    landing.close();
  }

  /** Mantém o gasto passado dentro do teto vitalício e só ativa depois do readback. */
  @Test
  void resumesThroughPendingClaimNativeBudgetAndCallback() throws Exception {
    worker.poll();
    assertThat(backend.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS).getPath())
        .isEqualTo("/api/facebook-campaign-resumptions/pending");
    assertThat(backend.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS).getPath())
        .isEqualTo("/api/facebook-campaign-resumptions/1/claim");
    assertThat(result).isNotNull();
    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(result.path("evidence").path("spend").decimalValue()).isEqualByComparingTo("27.45");
    assertThat(
            writes.stream()
                .filter(n -> n.has("lifetime_budget"))
                .findFirst()
                .orElseThrow()
                .path("lifetime_budget")
                .asText())
        .isEqualTo("15000");
    assertThat(writes).noneMatch(n -> "PAUSED".equals(n.path("status").asText()) && n.size() == 1);
    assertThat(writes.get(writes.size() - 1).path("status").asText()).isEqualTo("ACTIVE");
    assertThat(probes).isEqualTo(5);
    assertThat(campaignStatus).isEqualTo("ACTIVE");
  }

  /** Preserva o orçamento diário e aplica o teto acumulado no nível da campanha existente. */
  @Test
  void resumesDailyBudgetCampaignWithNativeCampaignCap() {
    dailyMode = true;
    worker.poll();
    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(result.path("evidence").path("budgetMode").asText())
        .isEqualTo("DAILY_WITH_CAMPAIGN_CAP");
    assertThat(result.path("evidence").path("campaignSpendCapMinor").asLong()).isEqualTo(15000);
    assertThat(result.path("evidence").path("dailyBudgetMinor").asLong()).isEqualTo(2000);
    assertThat(writes).anyMatch(n -> "15000".equals(n.path("spend_cap").asText()));
    assertThat(writes).anyMatch(n -> "2000".equals(n.path("daily_budget").asText()));
    assertThat(campaignStatus).isEqualTo("ACTIVE");
  }

  /** Substitui a hierarquia diária quando a Meta proíbe mudar seu tipo de orçamento. */
  @Test
  void usesReplacementAdSetLifetimeBudgetBelowCampaignMinimum() {
    dailyMode = true;
    campaignDailyBudget = 2000L;
    adSetDailyBudgetCleared = true;
    minimumCampaignSpendCap = 30000L;
    task.put("totalLimit", 125);

    worker.poll();

    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(result.path("evidence").path("budgetMode").asText())
        .isEqualTo("REPLACEMENT_ADSET_LIFETIME_BELOW_MINIMUM");
    assertThat(result.path("evidence").path("accountMinimumCampaignSpendCapMinor").asLong())
        .isEqualTo(30000L);
    assertThat(result.path("evidence").path("campaignLifetimeBudgetMinor").asLong()).isZero();
    assertThat(result.path("evidence").path("lifetimeBudgetMinor").asLong())
        .isEqualTo(9755L);
    assertThat(result.path("evidence").path("campaignDailyBudgetMinor").asLong()).isZero();
    assertThat(result.path("evidence").path("adSetDailyBudgetMinor").asLong()).isZero();
    assertThat(result.path("evidence").path("effectiveRemainingAverageMinor").asLong())
        .isEqualTo(1394L);
    assertThat(writes)
        .anyMatch(
            n ->
                "9755".equals(n.path("lifetime_budget").asText())
                    && !n.has("spend_cap")
                    && "PAUSED".equals(n.path("status").asText()));
    assertThat(writes).noneMatch(n -> n.has("lifetime_spend_cap"));
    assertThat(writes)
        .noneMatch(n -> n.has("daily_budget") && n.path("daily_budget").asLong() == 0L);
    JsonNode adSetCreation =
        java.util.stream.IntStream.range(0, writePaths.size())
            .filter(i -> writePaths.get(i).endsWith("/act_123456/adsets"))
            .mapToObj(writes::get)
            .findFirst()
            .orElseThrow();
    assertThat(adSetCreation.has("targeting_automation")).isFalse();
    assertThat(
            adSetCreation
                .path("targeting")
                .path("targeting_automation")
                .path("advantage_audience")
                .asInt())
        .isZero();
    assertThat(campaignStatus).isEqualTo("PAUSED");
    assertThat(replacementCampaignStatus).isEqualTo("ACTIVE");
    assertThat(result.path("replacement").path("sourceCampaignId").asText())
        .isEqualTo("campaign");
    assertThat(result.path("replacement").path("campaignId").asText())
        .isEqualTo(REPLACEMENT_CAMPAIGN_ID);
    assertThat(result.path("replacement").path("ads")).hasSize(1);
  }

  /** Compara o mínimo da Meta ao limite da campanha física, descontando histórico anterior. */
  @Test
  void usesReplacementWhenPhysicalCampaignLimitFallsBelowMinimum() {
    dailyMode = true;
    campaignDailyBudget = 2000L;
    adSetDailyBudgetCleared = true;
    minimumCampaignSpendCap = 30000L;
    task.put("historicalSpend", 25.50);
    task.put("totalLimit", 310);

    worker.poll();

    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(result.path("evidence").path("budgetMode").asText())
        .isEqualTo("REPLACEMENT_ADSET_LIFETIME_BELOW_MINIMUM");
    assertThat(result.path("evidence").path("historicalSpendMinor").asLong()).isEqualTo(2550L);
    assertThat(result.path("evidence").path("lifetimeBudgetMinor").asLong()).isEqualTo(14000L);
  }

  /** Recupera uma criação parcial pelo nome determinístico sem duplicar objetos Meta. */
  @Test
  void retriesReplacementWithoutDuplicatingHierarchy() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    task.put("totalLimit", 125);
    replacementCampaignCreated = true;
    replacementAdSetCreated = true;
    replacementAdCreated = true;
    replacementLifetimeBudget = 9755L;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(writePaths)
        .noneMatch(
            path ->
                path.endsWith("/act_123456/campaigns")
                    || path.endsWith("/act_123456/adsets")
                    || path.endsWith("/act_123456/ads"));
    assertThat(replacementCampaignStatus).isEqualTo("ACTIVE");
  }

  /** Bloqueia antes da ativação quando a Meta devolve outro saldo na hierarquia substituta. */
  @Test
  void divergentReplacementBudgetKeepsBothCampaignsPaused() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    wrongReplacementReadback = true;
    task.put("totalLimit", 125);

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("error").asText()).contains("conjunto substituto");
    assertThat(writes).anyMatch(n -> "9755".equals(n.path("lifetime_budget").asText()));
    assertThat(writes).noneMatch(n -> n.has("lifetime_spend_cap"));
    assertThat(campaignStatus).isEqualTo("PAUSED");
    assertThat(replacementCampaignStatus).isEqualTo("PAUSED");
  }

  /** Compensa a substituta mesmo quando a última conferência da origem falha após a ativação. */
  @Test
  void sourcePauseDivergenceAfterActivationPausesReplacement() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    sourcePauseDivergesAfterReplacementActivation = true;
    task.put("totalLimit", 125);

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("error").asText()).contains("campanha anterior");
    assertThat(campaignStatus).isEqualTo("PAUSED");
    assertThat(replacementCampaignStatus).isEqualTo("PAUSED");
    assertThat(result.path("evidence").path("compensation").asText()).isEqualTo("PAUSED");
  }

  /** Bloqueia antes da migração quando existe teto de campanha que não pode ser zerado. */
  @Test
  void existingCampaignSpendCapBlocksMigrationBeforeMutation() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    campaignSpendCap = 15000L;
    task.put("totalLimit", 125);

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("error").asText()).contains("spend_cap anterior");
    assertThat(writes).isEmpty();
    assertThat(campaignStatus).isEqualTo("PAUSED");
  }

  /** Retoma uma migração interrompida para vitalício sem duplicar campanha ou gasto. */
  @Test
  void completesPartiallyMigratedCampaignLifetimeBudget() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    task.put("totalLimit", 125);
    campaignLifetimeBudget = 12500L;
    adSetDailyBudgetCleared = true;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(result.path("evidence").path("budgetMode").asText())
        .isEqualTo("CAMPAIGN_LIFETIME_BELOW_MINIMUM");
    assertThat(result.path("evidence").path("campaignLifetimeBudgetMinor").asLong())
        .isEqualTo(12500L);
    assertThat(campaignStatus).isEqualTo("ACTIVE");
  }

  /** Bloqueia sem escrever quando a conta não confirma campanha nem exceção por mínimo. */
  @Test
  void missingNativeCapSupportFailsBeforeBudgetMutation() {
    dailyMode = true;
    campaignCapUsable = false;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("error").asText()).contains("teto nativo compatível");
    assertThat(writes).isEmpty();
    assertThat(campaignStatus).isEqualTo("PAUSED");
  }

  /** Não ativa quando a Meta aceita a escrita, mas devolve outro orçamento vitalício. */
  @Test
  void divergentCampaignLifetimeBudgetKeepsCampaignPaused() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    campaignLifetimeBudget = 12500L;
    adSetDailyBudgetCleared = true;
    wrongCampaignLifetimeBudget = true;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("error").asText()).contains("orçamento e teto acumulado");
    assertThat(campaignStatus).isEqualTo("PAUSED");
  }

  /** Não ativa quando a campanha devolve término diferente após a migração. */
  @Test
  void divergentCampaignStopTimeKeepsCampaignPaused() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    campaignLifetimeBudget = 12500L;
    adSetDailyBudgetCleared = true;
    task.put("totalLimit", 125);
    wrongCampaignStopTime = true;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("error").asText()).contains("orçamento e teto acumulado");
    assertThat(campaignStatus).isEqualTo("PAUSED");
  }

  /** Falha no orçamento vitalício da substituta preserva a origem pausada. */
  @Test
  void rejectedCampaignLifetimeBudgetKeepsPartialStatePaused() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    task.put("totalLimit", 125);
    rejectBudget = true;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(campaignStatus).isEqualTo("PAUSED");
    assertThat(replacementCampaignStatus).isEqualTo("PAUSED");
    assertThat(result.path("evidence").path("compensation").asText()).isEqualTo("PAUSED");
  }

  /** Pausa primeiro uma campanha recuperada ativa antes de reaplicar limites e reativá-la. */
  @Test
  void activeRecoveryIsPausedBeforeBudgetMutation() {
    campaignStatus = "ACTIVE";

    worker.poll();

    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(writes.get(0).path("status").asText()).isEqualTo("PAUSED");
    assertThat(writes.get(writes.size() - 1).path("status").asText()).isEqualTo("ACTIVE");
  }

  /** Rejeição da Meta não deve se transformar em sucesso nem consumir o orçamento novo. */
  @Test
  void rejectedBudgetKeepsCampaignPausedAndReportsFailure() {
    rejectBudget = true;
    worker.poll();
    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(campaignStatus).isEqualTo("PAUSED");
    assertThat(result.path("error").asText()).contains("code=100");
    assertThat(result.path("evidence").path("metaError").path("httpStatus").asInt()).isEqualTo(400);
    assertThat(
            result
                .path("evidence")
                .path("metaError")
                .path("response")
                .path("error")
                .path("error_subcode")
                .asInt())
        .isEqualTo(2446307);
  }

  /** Resposta HTTP aceita sem confirmação do valor nativo não libera veiculação. */
  @Test
  void divergentReadbackNeverActivates() {
    wrongReadback = true;
    worker.poll();
    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(writes)
        .noneMatch(n -> "ACTIVE".equals(n.path("status").asText()) && !n.has("lifetime_budget"));
  }

  /** Ausência de gasto na fonte oficial é indisponibilidade, nunca um novo saldo de 150. */
  @Test
  void emptyInsightsBlocksInsteadOfResettingSpend() {
    emptyInsights = true;
    worker.poll();
    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(writes).noneMatch(n -> n.has("lifetime_budget"));
  }

  /** Monta a hierarquia de origem com todos os campos necessários para clonagem segura. */
  private String sourceCampaignJson() {
    ObjectNode campaign = json.createObjectNode();
    campaign.put("id", "campaign");
    campaign.put("name", "Campaign");
    campaign.put("objective", "OUTCOME_SALES");
    campaign.putArray("special_ad_categories");
    campaign.putArray("special_ad_category_country");
    campaign.put("status", campaignStatus);
    campaign.put("effective_status", campaignStatus);
    campaign.put("spend_cap", campaignSpendCap);
    campaign.put("daily_budget", campaignDailyBudget);
    campaign.put(
        "lifetime_budget",
        wrongCampaignLifetimeBudget && campaignLifetimeBudget > 0
            ? campaignLifetimeBudget - 100
            : campaignLifetimeBudget);
    campaign.put("can_use_spend_cap", campaignCapUsable);
    campaign.put("account_id", "123456");
    ObjectNode adSet = json.createObjectNode();
    adSet.put("id", "adset");
    adSet.put("name", "Ad set");
    adSet.put("status", "ACTIVE");
    adSet.put("effective_status", "CAMPAIGN_PAUSED");
    adSet.put("lifetime_budget", dailyMode ? 0L : 10000L);
    adSet.put("lifetime_spend_cap", adSetLifetimeSpendCap);
    adSet.put("daily_budget", dailyMode && !adSetDailyBudgetCleared ? 2000L : 0L);
    adSet.put("daily_spend_cap", 0L);
    adSet.put("end_time", finalEnd);
    adSet.put("billing_event", "IMPRESSIONS");
    adSet.put("optimization_goal", "OFFSITE_CONVERSIONS");
    adSet.put("destination_type", "WEBSITE");
    adSet.put("bid_strategy", "LOWEST_COST_WITHOUT_CAP");
    ObjectNode targeting = adSet.putObject("targeting");
    targeting.putObject("geo_locations").putArray("countries").add("BR");
    targeting.putArray("publisher_platforms").add("instagram");
    targeting.putObject("targeting_automation").put("advantage_audience", 0);
    ObjectNode promoted = adSet.putObject("promoted_object");
    promoted.put("pixel_id", "1272936690700110");
    promoted.put("custom_event_type", "PURCHASE");
    ObjectNode attribution = adSet.putArray("attribution_spec").addObject();
    attribution.put("event_type", "CLICK_THROUGH");
    attribution.put("window_days", 7);
    ObjectNode sourceAd = adSet.putObject("ads").putArray("data").addObject();
    sourceAd.put("id", SOURCE_AD_ID);
    sourceAd.put("name", "Ad");
    sourceAd.put("status", "ACTIVE");
    sourceAd.putObject("creative").put("id", SOURCE_CREATIVE_ID);
    campaign.putObject("adsets").putArray("data").add(adSet);
    return campaign.toString();
  }

  /** Monta a releitura da campanha substituta com orçamento exclusivamente no conjunto. */
  private String replacementCampaignJson() {
    return "{\"id\":\""
        + REPLACEMENT_CAMPAIGN_ID
        + "\",\"name\":\"Campaign · retomada 1\",\"status\":\""
        + replacementCampaignStatus
        + "\",\"effective_status\":\""
        + replacementCampaignStatus
        + "\",\"objective\":\"OUTCOME_SALES\",\"spend_cap\":\"0\",\"daily_budget\":\"0\",\"lifetime_budget\":\"0\"}";
  }

  /** Monta a releitura do conjunto substituto com prazo e saldo vitalício exatos. */
  private String replacementAdSetJson() {
    long readbackBudget =
        wrongReplacementReadback && replacementLifetimeBudget > 0L
            ? replacementLifetimeBudget - 100L
            : replacementLifetimeBudget;
    return "{\"id\":\""
        + REPLACEMENT_AD_SET_ID
        + "\",\"name\":\"Ad set · retomada 1\",\"status\":\""
        + replacementAdSetStatus
        + "\",\"effective_status\":\""
        + ("ACTIVE".equals(replacementCampaignStatus) ? replacementAdSetStatus : "CAMPAIGN_PAUSED")
        + "\",\"campaign_id\":\""
        + REPLACEMENT_CAMPAIGN_ID
        + "\",\"daily_budget\":\"0\",\"lifetime_budget\":\""
        + readbackBudget
        + "\",\"daily_spend_cap\":\"0\",\"lifetime_spend_cap\":\"0\",\"end_time\":\""
        + finalEnd
        + "\"}";
  }

  /** Monta a releitura do anúncio substituto que reutiliza o criativo aprovado. */
  private String replacementAdJson() {
    return "{\"id\":\""
        + REPLACEMENT_AD_ID
        + "\",\"name\":\"Ad · retomada 1 · origem "
        + SOURCE_AD_ID.substring(SOURCE_AD_ID.length() - 8)
        + "\",\"status\":\"ACTIVE\",\"effective_status\":\"CAMPAIGN_PAUSED\",\"adset_id\":\""
        + REPLACEMENT_AD_SET_ID
        + "\",\"creative\":{\"id\":\""
        + SOURCE_CREATIVE_ID
        + "\"}}";
  }

  /** Cria resposta JSON para a integração simulada. */
  private MockResponse ok(String body) {
    return new MockResponse().addHeader("Content-Type", "application/json").setBody(body);
  }

  /** Indisponibilidade da fila não permite acessar ou modificar mídia. */
  @Test
  void unavailableBackendNeverWritesToMeta() {
    backendUnavailable = true;
    worker.poll();
    assertThat(writes).isEmpty();
    assertThat(result).isNull();
  }

  /** Falha ao confirmar no backend desfaz a ativação e persiste falha quando a conexão retorna. */
  @Test
  void callbackFailureCompensatesActivation() {
    rejectSuccessCallback = true;
    worker.poll();
    assertThat(campaignStatus).isEqualTo("PAUSED");
    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("evidence").path("compensation").asText()).isEqualTo("PAUSED");
  }

  /** Resposta perdida após commit não pausa uma campanha que o backend já confirmou. */
  @Test
  void committedCallbackResponseLossPreservesActivation() {
    rejectSuccessCallback = true;
    successCallbackCommittedDespiteFailure = true;

    worker.poll();

    assertThat(campaignStatus).isEqualTo("ACTIVE");
    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(result.path("evidence").path("compensation").isMissingNode()).isTrue();
  }
}
