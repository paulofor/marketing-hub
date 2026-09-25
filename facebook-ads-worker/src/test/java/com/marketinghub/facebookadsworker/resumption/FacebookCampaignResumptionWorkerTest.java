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
  private static final org.slf4j.Logger LOG =
      org.slf4j.LoggerFactory.getLogger(FacebookCampaignResumptionWorkerTest.class);
  private final ObjectMapper json = new ObjectMapper();
  private MockWebServer meta, backend, landing;
  private FacebookCampaignResumptionWorker worker;
  private ObjectNode task;
  private JsonNode result;
  private final List<JsonNode> writes = new CopyOnWriteArrayList<>();
  private boolean rejectBudget,
      wrongReadback,
      wrongCampaignDailyBudget,
      wrongAdSetCap,
      emptyInsights,
      backendUnavailable,
      rejectSuccessCallback,
      retainAdSetDailyBudgetAfterCampaignBudget;
  private boolean dailyMode;
  private boolean campaignCapUsable = true;
  private String campaignStatus = "PAUSED";
  private long campaignSpendCap;
  private long campaignDailyBudget;
  private long adSetLifetimeSpendCap;
  private long minimumCampaignSpendCap;
  private boolean adSetDailyBudgetCleared;
  private String finalEnd;
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
                if (path.endsWith("/campaign") && body.has("status"))
                  campaignStatus = body.path("status").asText();
                if (path.endsWith("/campaign") && body.has("spend_cap"))
                  campaignSpendCap = body.path("spend_cap").asLong();
                if (path.endsWith("/campaign") && body.has("daily_budget")) {
                  campaignDailyBudget = body.path("daily_budget").asLong();
                  if (!retainAdSetDailyBudgetAfterCampaignBudget) adSetDailyBudgetCleared = true;
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
                            + (adSetLifetimeSpendCap + (wrongAdSetCap ? 100 : 0))
                            + "\",\"end_time\":\""
                            + finalEnd
                            + "\"}")
                    : ok(
                        "{\"id\":\"adset\",\"status\":\"ACTIVE\",\"daily_budget\":\"0\",\"lifetime_budget\":\""
                            + (wrongReadback ? 10000 : 15000)
                            + "\",\"end_time\":\""
                            + finalEnd
                            + "\"}");
              if (fields.contains("adsets"))
                return dailyMode
                    ? ok(
                        "{\"id\":\"campaign\",\"status\":\""
                            + campaignStatus
                            + "\",\"effective_status\":\""
                            + campaignStatus
                            + "\",\"spend_cap\":\""
                            + campaignSpendCap
                            + "\",\"daily_budget\":\""
                            + (wrongCampaignDailyBudget && campaignDailyBudget > 0
                                ? campaignDailyBudget - 100
                                : campaignDailyBudget)
                            + "\",\"lifetime_budget\":\"0\",\"can_use_spend_cap\":"
                            + campaignCapUsable
                            + ",\"account_id\":\"123456\",\"adsets\":{\"data\":[{\"id\":\"adset\",\"lifetime_budget\":\"0\",\"lifetime_spend_cap\":\""
                            + adSetLifetimeSpendCap
                            + "\",\"daily_budget\":\""
                            + (adSetDailyBudgetCleared ? 0 : 2000)
                            + "\"}]}}")
                    : ok(
                        "{\"id\":\"campaign\",\"status\":\""
                            + campaignStatus
                            + "\",\"effective_status\":\""
                            + campaignStatus
                            + "\",\"can_use_spend_cap\":true,\"account_id\":\"123456\",\"adsets\":{\"data\":[{\"id\":\"adset\",\"lifetime_budget\":\"10000\",\"lifetime_spend_cap\":\"0\",\"daily_budget\":\"0\"}]}}");
              if ("id,status,spend_cap,daily_budget,lifetime_budget".equals(fields))
                return ok(
                    "{\"id\":\"campaign\",\"status\":\"PAUSED\",\"spend_cap\":\""
                        + campaignSpendCap
                        + "\",\"daily_budget\":\""
                        + (wrongCampaignDailyBudget && campaignDailyBudget > 0
                            ? campaignDailyBudget - 100
                            : campaignDailyBudget)
                        + "\",\"lifetime_budget\":\"0\"}");
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

  /** Migra o diário, relê a remoção automática e aplica teto no conjunto abaixo do mínimo. */
  @Test
  void preservesDailyBudgetWithAdSetLifetimeCapBelowCampaignMinimum() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    task.put("totalLimit", 125);

    worker.poll();

    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(result.path("evidence").path("budgetMode").asText())
        .isEqualTo("CAMPAIGN_DAILY_WITH_ADSET_LIFETIME_CAP");
    assertThat(result.path("evidence").path("accountMinimumCampaignSpendCapMinor").asLong())
        .isEqualTo(30000L);
    assertThat(result.path("evidence").path("adSetLifetimeSpendCapMinor").asLong())
        .isEqualTo(12500L);
    assertThat(result.path("evidence").path("campaignDailyBudgetMinor").asLong()).isEqualTo(2000L);
    assertThat(result.path("evidence").path("adSetDailyBudgetMinor").asLong()).isZero();
    assertThat(writes)
        .anyMatch(
            n ->
                "2000".equals(n.path("daily_budget").asText())
                    && !n.has("spend_cap")
                    && "PAUSED".equals(n.path("status").asText()));
    assertThat(writes)
        .anyMatch(
            n -> !n.has("daily_budget") && "12500".equals(n.path("lifetime_spend_cap").asText()));
    assertThat(writes)
        .noneMatch(n -> n.has("daily_budget") && n.path("daily_budget").asLong() == 0L);
    assertThat(campaignStatus).isEqualTo("ACTIVE");
  }

  /** Bloqueia antes do teto quando a Meta não remove o orçamento próprio do conjunto. */
  @Test
  void retainedAdSetDailyBudgetBlocksCapBeforeSecondMutation() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    retainAdSetDailyBudgetAfterCampaignBudget = true;
    task.put("totalLimit", 125);

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("error").asText()).contains("não removeu o orçamento próprio");
    assertThat(writes).anyMatch(n -> "2000".equals(n.path("daily_budget").asText()));
    assertThat(writes).noneMatch(n -> n.has("lifetime_spend_cap"));
    assertThat(campaignStatus).isEqualTo("PAUSED");
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

  /** Retoma uma migração interrompida depois de mover o diário, sem duplicar campanha ou gasto. */
  @Test
  void completesPartiallyMigratedCampaignDailyBudget() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    task.put("totalLimit", 125);
    campaignDailyBudget = 2000L;
    adSetDailyBudgetCleared = true;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(result.path("evidence").path("budgetMode").asText())
        .isEqualTo("CAMPAIGN_DAILY_WITH_ADSET_LIFETIME_CAP");
    assertThat(result.path("evidence").path("adSetLifetimeSpendCapMinor").asLong())
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

  /** Não ativa quando a Meta aceita a escrita, mas devolve outro teto no conjunto diário. */
  @Test
  void divergentAdSetLifetimeCapKeepsCampaignPaused() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    wrongAdSetCap = true;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("error").asText()).contains("teto acumulado");
    assertThat(campaignStatus).isEqualTo("PAUSED");
  }

  /** Não ativa quando a campanha devolve orçamento diário diferente após a migração. */
  @Test
  void divergentCampaignDailyBudgetKeepsCampaignPaused() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    task.put("totalLimit", 125);
    wrongCampaignDailyBudget = true;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("error").asText()).contains("orçamento diário");
    assertThat(campaignStatus).isEqualTo("PAUSED");
  }

  /** Falha no teto após migrar o diário preserva a campanha pausada para retry seguro. */
  @Test
  void rejectedAdSetCapAfterCampaignMigrationKeepsPartialStatePaused() {
    dailyMode = true;
    minimumCampaignSpendCap = 30000L;
    task.put("totalLimit", 125);
    rejectBudget = true;

    worker.poll();

    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(campaignDailyBudget).isEqualTo(2000L);
    assertThat(adSetLifetimeSpendCap).isZero();
    assertThat(result.path("evidence").path("compensation").asText()).isEqualTo("PAUSED");
    assertThat(campaignStatus).isEqualTo("PAUSED");
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
}
