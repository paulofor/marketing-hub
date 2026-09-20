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
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FacebookCampaignResumptionWorkerTest.class);
  private final ObjectMapper json = new ObjectMapper();
  private MockWebServer meta, backend, landing;
  private FacebookCampaignResumptionWorker worker;
  private ObjectNode task;
  private JsonNode result;
  private final List<JsonNode> writes = new CopyOnWriteArrayList<>();
  private boolean rejectBudget, wrongReadback, emptyInsights, backendUnavailable, rejectSuccessCallback;
  private String campaignStatus = "PAUSED";
  private String finalEnd;
  private int probes;

  /** Inicia três dependências HTTP descartáveis e uma autorização sintética. */
  @BeforeEach void setup() throws Exception {
    meta = new MockWebServer(); backend = new MockWebServer(); landing = new MockWebServer();
    meta.start(); backend.start(); landing.start();
    var endDate = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(6);
    finalEnd = endDate.atTime(23,59,59).atZone(ZoneId.of("America/Sao_Paulo")).toInstant().toString().replace("Z", "+0000");
    task = json.createObjectNode().put("id",1).put("experimentId",91).put("campaignId","campaign").put("adSetId","adset")
        .put("totalLimit",150).put("endDate",endDate.toString()).put("leaseToken","test-lease").put("destinationUrl",landing.url("/").toString());
    landing.setDispatcher(new Dispatcher() {
      /** Confirma que todas as sondas estão segregadas do tráfego comercial. */
      @Override public MockResponse dispatch(RecordedRequest request) {
        if (!request.getPath().contains("mh_audit=")) return new MockResponse().setResponseCode(400);
        probes++; return new MockResponse().addHeader("Content-Type","text/html").setBody("<html>" + "Teste funcional ".repeat(40) + "</html>");
      }
    });
    backend.setDispatcher(new Dispatcher() {
      /** Simula fila, reserva e persistência do callback oficial. */
      @Override public MockResponse dispatch(RecordedRequest request) {
        try {
          if (request.getPath().endsWith("/pending")) return backendUnavailable ? new MockResponse().setResponseCode(503) : ok("[" + task + "]");
          if (request.getPath().endsWith("/claim")) return ok(task.toString());
          if (request.getPath().endsWith("/result")) { result = json.readTree(request.getBody().readUtf8()); if (rejectSuccessCallback && result.path("success").asBoolean()) return new MockResponse().setResponseCode(503); return ok("{}"); }
          return new MockResponse().setResponseCode(404);
        } catch(Exception ex) { LOG.error("Falha na integração HTTP simulada: endpoint={}", request.getPath(), ex); throw new AssertionError(ex); }
      }
    });
    meta.setDispatcher(new Dispatcher() {
      /** Mantém estado Meta simulado para verificar ordem, moeda, limites e confirmação. */
      @Override public MockResponse dispatch(RecordedRequest request) {
        try {
          String path = request.getRequestUrl().encodedPath();
          if ("POST".equals(request.getMethod())) {
            JsonNode body = json.readTree(request.getBody().readUtf8()); writes.add(body);
            if (body.has("lifetime_budget") && rejectBudget) return new MockResponse().setResponseCode(400).setBody("{\"error\":\"invalid budget\"}");
            if (path.endsWith("/campaign") && body.has("status")) campaignStatus=body.path("status").asText();
            return ok("{\"success\":true}");
          }
          if (path.endsWith("/insights")) {
            assertThat(json.readTree(request.getRequestUrl().queryParameter("time_range")).path("since").asText()).isEqualTo("2026-09-07");
            return ok(emptyInsights ? "{\"data\":[]}" : "{\"data\":[{\"spend\":\"27.45\"}]}");
          }
          String fields = request.getRequestUrl().queryParameter("fields");
          if ("currency".equals(fields)) return ok("{\"currency\":\"BRL\"}");
          if ("start_time".equals(fields)) return ok("{\"start_time\":\"2026-09-07T00:13:13-0300\"}");
          if (path.endsWith("/adset")) return ok("{\"id\":\"adset\",\"status\":\"ACTIVE\",\"daily_budget\":\"0\",\"lifetime_budget\":\""+(wrongReadback?10000:15000)+"\",\"end_time\":\""+finalEnd+"\"}");
          if (fields.contains("adsets")) return ok("{\"id\":\"campaign\",\"status\":\"PAUSED\",\"account_id\":\"123456\",\"adsets\":{\"data\":[{\"id\":\"adset\",\"lifetime_budget\":\"10000\",\"daily_budget\":\"0\"}]}}");
          return ok("{\"id\":\"campaign\",\"status\":\""+campaignStatus+"\",\"effective_status\":\""+campaignStatus+"\"}");
        } catch(Exception ex) { LOG.error("Falha na integração HTTP simulada: endpoint={}", request.getPath(), ex); throw new AssertionError(ex); }
      }
    });
    var config = mock(FacebookWorkerConfigurationClient.class);
    when(config.fetchConfiguration()).thenReturn(Optional.of(json.treeToValue(json.readTree("{\"accessToken\":\"synthetic-test-only\"}"),FacebookWorkerConfigurationClient.FacebookWorkerConfiguration.class)));
    worker = new FacebookCampaignResumptionWorker(WebClient.builder(),config,json,backend.url("/").toString().replaceAll("/$",""),meta.url("/").toString().replaceAll("/$",""),"v23.0");
  }

  /** Fecha todas as dependências efêmeras da homologação. */
  @AfterEach void cleanup() throws Exception { meta.close(); backend.close(); landing.close(); }

  /** Mantém o gasto passado dentro do teto vitalício e só ativa depois do readback. */
  @Test void resumesThroughPendingClaimNativeBudgetAndCallback() throws Exception {
    worker.poll();
    assertThat(backend.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS).getPath()).isEqualTo("/api/facebook-campaign-resumptions/pending");
    assertThat(backend.takeRequest(1, java.util.concurrent.TimeUnit.SECONDS).getPath()).isEqualTo("/api/facebook-campaign-resumptions/1/claim");
    assertThat(result).isNotNull(); assertThat(result.path("success").asBoolean()).isTrue();
    assertThat(result.path("evidence").path("spend").decimalValue()).isEqualByComparingTo("27.45");
    assertThat(writes.stream().filter(n -> n.has("lifetime_budget")).findFirst().orElseThrow().path("lifetime_budget").asText()).isEqualTo("15000");
    assertThat(writes.get(0).path("status").asText()).isEqualTo("PAUSED");
    assertThat(writes.get(writes.size()-1).path("status").asText()).isEqualTo("ACTIVE");
    assertThat(probes).isEqualTo(5); assertThat(campaignStatus).isEqualTo("ACTIVE");
  }

  /** Rejeição da Meta não deve se transformar em sucesso nem consumir o orçamento novo. */
  @Test void rejectedBudgetKeepsCampaignPausedAndReportsFailure() {
    rejectBudget=true; worker.poll(); assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(campaignStatus).isEqualTo("PAUSED"); assertThat(result.path("error").asText()).isNotBlank();
  }

  /** Resposta HTTP aceita sem confirmação do valor nativo não libera veiculação. */
  @Test void divergentReadbackNeverActivates() {
    wrongReadback=true; worker.poll(); assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(writes).noneMatch(n -> "ACTIVE".equals(n.path("status").asText()) && !n.has("lifetime_budget"));
  }

  /** Ausência de gasto na fonte oficial é indisponibilidade, nunca um novo saldo de 150. */
  @Test void emptyInsightsBlocksInsteadOfResettingSpend() {
    emptyInsights=true; worker.poll(); assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(writes).noneMatch(n -> n.has("lifetime_budget"));
  }

  /** Cria resposta JSON para a integração simulada. */
  private MockResponse ok(String body) { return new MockResponse().addHeader("Content-Type","application/json").setBody(body); }
  /** Indisponibilidade da fila não permite acessar ou modificar mídia. */
  @Test void unavailableBackendNeverWritesToMeta() {
    backendUnavailable=true; worker.poll(); assertThat(writes).isEmpty(); assertThat(result).isNull();
  }

  /** Falha ao confirmar no backend desfaz a ativação e persiste falha quando a conexão retorna. */
  @Test void callbackFailureCompensatesActivation() {
    rejectSuccessCallback=true; worker.poll(); assertThat(campaignStatus).isEqualTo("PAUSED");
    assertThat(result.path("success").asBoolean()).isFalse();
    assertThat(result.path("evidence").path("compensation").asText()).isEqualTo("PAUSED");
  }

}
