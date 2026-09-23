package com.marketinghub.facebookadsworker.resumption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Executa retomadas autorizadas e confirma orçamento nativo antes de ativar a campanha existente.
 */
@Component
public class FacebookCampaignResumptionWorker {
  private static final Logger LOG = LoggerFactory.getLogger(FacebookCampaignResumptionWorker.class);
  private final WebClient backend;
  private final WebClient meta;
  private final FacebookWorkerConfigurationClient configuration;
  private final ObjectMapper json;
  private final String apiVersion;
  private final HttpClient publicHttp =
      HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.NORMAL)
          .connectTimeout(Duration.ofSeconds(4))
          .build();

  /** Configura clientes oficiais sem registrar ou persistir credenciais. */
  public FacebookCampaignResumptionWorker(
      WebClient.Builder builder,
      FacebookWorkerConfigurationClient configuration,
      ObjectMapper json,
      @Value("${backend.base-url:http://localhost:8000}") String backendUrl,
      @Value("${facebook.graph-api.base-url:https://graph.facebook.com}") String metaUrl,
      @Value("${facebook.graph-api.version:v23.0}") String apiVersion) {
    this.backend =
        builder.clone().baseUrl(backendUrl + "/api/facebook-campaign-resumptions").build();
    this.meta = builder.clone().baseUrl(metaUrl).build();
    this.configuration = configuration;
    this.json = json;
    this.apiVersion = apiVersion;
  }

  /** Consome somente a fila canônica e reserva cada autorização antes de executar. */
  @Scheduled(cron = "0 * * * * *")
  public void poll() {
    try {
      JsonNode pending =
          backend
              .get()
              .uri("/pending")
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block(Duration.ofSeconds(30));
      if (pending == null || !pending.isArray() || pending.isEmpty()) return;
      var config =
          configuration
              .fetchConfiguration()
              .orElseThrow(() -> new IllegalStateException("Configuração Meta indisponível"));
      if (config.accessToken() == null || config.accessToken().isBlank())
        throw new IllegalStateException("Credencial Meta indisponível");
      for (JsonNode item : pending) {
        long id = item.path("id").asLong();
        try {
          JsonNode claimed =
              backend
                  .post()
                  .uri("/" + id + "/claim")
                  .retrieve()
                  .bodyToMono(JsonNode.class)
                  .block(Duration.ofSeconds(30));
          execute(claimed, config.accessToken());
        } catch (Exception ex) {
          LOG.error(
              "Falha consumindo retomada: requestId={} endpoint=/api/facebook-campaign-resumptions",
              id,
              ex);
        }
      }
    } catch (Exception ex) {
      LOG.error("Falha consultando fila de retomadas Facebook", ex);
    }
  }

  /** Aplica a autorização idempotente e compensa falhas mantendo a campanha pausada. */
  public void execute(JsonNode task, String token) {
    String campaignId = task.path("campaignId").asText();
    long requestId = task.path("id").asLong();
    ObjectNode evidence = json.createObjectNode();
    try {
      String adSetId = task.path("adSetId").asText();
      BigDecimal cap = task.path("totalLimit").decimalValue();
      BigDecimal dailyBudget = task.path("dailyBudget").decimalValue();
      LocalDate startDate = LocalDate.parse(task.path("startDate").asText());
      LocalDate endDate = LocalDate.parse(task.path("endDate").asText());
      Instant end = endDate.atTime(23, 59, 59).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
      LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
      if (today.isBefore(startDate)
          || today.isAfter(endDate)
          || cap.signum() <= 0
          || dailyBudget.signum() <= 0
          || dailyBudget.compareTo(cap) > 0)
        throw new IllegalStateException("Autorização financeira fora da janela ou inválida");
      long minor = cap.movePointRight(2).longValueExact();
      long dailyMinor = dailyBudget.movePointRight(2).longValueExact();
      JsonNode before =
          get(
              campaignId,
              "id,status,spend_cap,account_id,adsets.limit(2){id,status,effective_status,lifetime_budget,daily_budget,end_time}",
              token,
              requestId);
      evidence.set("before", before);
      String accountId = before.path("account_id").asText();
      if (!accountId.matches("[0-9]+"))
        throw new IllegalStateException("Conta Meta não confirmada");
      JsonNode account = get("act_" + accountId, "currency", token, requestId);
      evidence.set("account", account);
      JsonNode sets = before.path("adsets").path("data");
      if (!"BRL".equals(account.path("currency").asText())
          || !sets.isArray()
          || sets.size() != 1
          || !adSetId.equals(sets.get(0).path("id").asText()))
        throw new IllegalStateException("Retomada exige moeda BRL e um único conjunto autorizado");
      JsonNode beforeAdSet = sets.get(0);
      boolean lifetimeMode =
          beforeAdSet.path("lifetime_budget").asLong() > 0
              && beforeAdSet.path("daily_budget").asLong() == 0;
      boolean dailyMode =
          beforeAdSet.path("daily_budget").asLong() > 0
              && beforeAdSet.path("lifetime_budget").asLong() == 0;
      if (!lifetimeMode && !dailyMode)
        throw new IllegalStateException(
            "Modo de orçamento do conjunto não permite retomada segura");
      if (!"PAUSED".equals(before.path("status").asText())
          && !"ACTIVE".equals(before.path("status").asText()))
        throw new IllegalStateException("Estado da campanha não permite retomada");
      // Pausa também uma execução recuperada antes de reaplicar ou verificar qualquer limite.
      post(campaignId, Map.of("status", "PAUSED"), token, requestId);
      JsonNode insights = insights(campaignId, token, requestId);
      evidence.set("insights", insights);
      JsonNode rows = insights.path("data");
      if (!rows.isArray() || rows.size() != 1 || !rows.get(0).hasNonNull("spend"))
        throw new IllegalStateException("Gasto acumulado não confirmado pela Meta");
      BigDecimal spend = new BigDecimal(rows.get(0).path("spend").asText());
      if (spend.compareTo(cap) >= 0)
        throw new IllegalStateException("Teto autorizado já consumido");
      verifyDestination(task.path("destinationUrl").asText(), requestId);
      String budgetMode;
      if (lifetimeMode) {
        budgetMode = "LIFETIME";
        if (before.path("spend_cap").asLong() > 0)
          post(campaignId, Map.of("spend_cap", Long.toString(minor)), token, requestId);
        post(
            adSetId,
            Map.of(
                "lifetime_budget",
                Long.toString(minor),
                "end_time",
                end.toString(),
                "status",
                "ACTIVE"),
            token,
            requestId);
      } else {
        budgetMode = "DAILY_WITH_CAMPAIGN_CAP";
        post(campaignId, Map.of("spend_cap", Long.toString(minor)), token, requestId);
        post(
            adSetId,
            Map.of(
                "daily_budget",
                Long.toString(dailyMinor),
                "end_time",
                end.toString(),
                "status",
                "ACTIVE"),
            token,
            requestId);
      }
      JsonNode verified =
          get(adSetId, "id,status,lifetime_budget,daily_budget,end_time", token, requestId);
      evidence.set("verifiedAdSet", verified);
      if (!"ACTIVE".equals(verified.path("status").asText())
          || !OffsetDateTime.parse(
                  verified.path("end_time").asText(),
                  java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX"))
              .toInstant()
              .equals(end))
        throw new IllegalStateException(
            "Meta não confirmou estado e prazo autorizados do conjunto");
      JsonNode verifiedCampaign = get(campaignId, "id,status,spend_cap", token, requestId);
      evidence.set("verifiedCampaign", verifiedCampaign);
      if (lifetimeMode
          && (verified.path("lifetime_budget").asLong(-1) != minor
              || verified.path("daily_budget").asLong() != 0))
        throw new IllegalStateException("Meta não confirmou orçamento vitalício autorizado");
      if (dailyMode
          && (verified.path("daily_budget").asLong(-1) != dailyMinor
              || verified.path("lifetime_budget").asLong() != 0
              || verifiedCampaign.path("spend_cap").asLong(-1) != minor))
        throw new IllegalStateException("Meta não confirmou orçamento diário e teto acumulado");
      post(campaignId, Map.of("status", "ACTIVE"), token, requestId);
      JsonNode active = get(campaignId, "id,status,effective_status", token, requestId);
      evidence.set("after", active);
      if (!"ACTIVE".equals(active.path("status").asText())
          || !"ACTIVE".equals(active.path("effective_status").asText()))
        throw new IllegalStateException("Meta ainda não confirmou campanha ativa");
      evidence.put("campaignId", campaignId);
      evidence.put("campaignStatus", "ACTIVE");
      evidence.put("adSetId", adSetId);
      evidence.put("budgetMode", budgetMode);
      evidence.put("campaignSpendCapMinor", verifiedCampaign.path("spend_cap").asLong());
      evidence.put("dailyBudgetMinor", verified.path("daily_budget").asLong());
      evidence.put("lifetimeBudgetMinor", verified.path("lifetime_budget").asLong());
      evidence.put("startDate", startDate.toString());
      evidence.put("endDate", endDate.toString());
      evidence.put("spend", spend);
      result(task, true, null, evidence);
    } catch (Exception ex) {
      LOG.error("Falha retomando campanha: requestId={} campaignId={}", requestId, campaignId, ex);
      try {
        post(campaignId, Map.of("status", "PAUSED"), token, requestId);
        evidence.put("compensation", "PAUSED");
      } catch (Exception pauseEx) {
        LOG.error(
            "Falha na pausa compensatória: requestId={} campaignId={}",
            requestId,
            campaignId,
            pauseEx);
        evidence.put("compensation", "PAUSE_UNCONFIRMED");
      }
      try {
        result(task, false, ex.getMessage(), evidence);
      } catch (Exception callbackEx) {
        LOG.error(
            "Falha registrando resultado: requestId={} campaignId={}",
            requestId,
            campaignId,
            callbackEx);
      }
    }
  }

  /** Consulta evidência nativa com autenticação em header e auditoria sem token. */
  private JsonNode get(String id, String fields, String token, long requestId) {
    JsonNode response =
        meta.get()
            .uri(
                b ->
                    b.path("/" + apiVersion + "/" + id)
                        .queryParam("fields", "{fields}")
                        .build(fields))
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    LOG.info(
        "Retomada Meta GET: requestId={} endpoint=/{}/{} fields={} response={}",
        requestId,
        apiVersion,
        id,
        fields,
        response);
    return response;
  }

  /** Obtém gasto acumulado desde a criação, sem depender do preset que omite o dia corrente. */
  private JsonNode insights(String campaignId, String token, long requestId) {
    JsonNode campaign = get(campaignId, "start_time", token, requestId);
    String since = campaign.path("start_time").asText();
    if (since.length() < 10) throw new IllegalStateException("Data de início Meta ausente");
    String range =
        "{\"since\":\""
            + since.substring(0, 10)
            + "\",\"until\":\""
            + LocalDate.now(ZoneId.of("America/Sao_Paulo"))
            + "\"}";
    JsonNode response =
        meta.get()
            .uri(
                b ->
                    b.path("/" + apiVersion + "/" + campaignId + "/insights")
                        .queryParam("fields", "spend")
                        .queryParam("time_range", "{range}")
                        .queryParam("time_increment", "all_days")
                        .build(range))
            .headers(h -> h.setBearerAuth(token))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    LOG.info(
        "Retomada Meta Insights: requestId={} endpoint=/{}/{}/insights range={} response={}",
        requestId,
        apiVersion,
        campaignId,
        range,
        response);
    return response;
  }

  /** Escreve parâmetros autorizados e exige confirmação explícita da Meta. */
  private void post(String id, Map<String, String> body, String token, long requestId) {
    JsonNode response =
        meta.post()
            .uri("/" + apiVersion + "/" + id)
            .headers(h -> h.setBearerAuth(token))
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block(Duration.ofSeconds(30));
    LOG.info(
        "Retomada Meta POST: requestId={} endpoint=/{}/{} request={} response={}",
        requestId,
        apiVersion,
        id,
        body,
        response);
    if (response == null || !response.path("success").asBoolean())
      throw new IllegalStateException("Meta não confirmou atualização");
  }

  /** Confirma cinco respostas públicas rápidas sem contaminar analytics comerciais. */
  private void verifyDestination(String url, long requestId) throws Exception {
    URI uri = URI.create(url + (url.contains("?") ? "&" : "?") + "mh_audit=resume-" + requestId);
    if (!java.util.Set.of("http", "https").contains(uri.getScheme()))
      throw new IllegalStateException("Destino público inválido");
    for (int i = 0; i < 5; i++) {
      long start = System.nanoTime();
      var response =
          publicHttp.send(
              HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(4)).GET().build(),
              HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200
          || !response.headers().firstValue("content-type").orElse("").contains("text/html")
          || response.body().length() < 200
          || Duration.ofNanos(System.nanoTime() - start).toMillis() >= 4000)
        throw new IllegalStateException("Destino público falhou na verificação de retomada");
    }
  }

  /** Envia evidência estruturada e mantém correlação exclusiva da autorização. */
  private void result(JsonNode task, boolean success, String error, ObjectNode evidence) {
    ObjectNode body = json.createObjectNode();
    body.put("leaseToken", task.path("leaseToken").asText());
    body.put("success", success);
    body.put("error", error);
    body.set("evidence", evidence);
    backend
        .post()
        .uri("/" + task.path("id").asLong() + "/result")
        .bodyValue(body)
        .retrieve()
        .toBodilessEntity()
        .block(Duration.ofSeconds(30));
  }
}
