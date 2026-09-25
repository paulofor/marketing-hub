package com.marketinghub.facebookadsworker.resumption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
  private final String backendApiUrl;
  private final String metaApiUrl;
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
    this.backendApiUrl = trimSlash(backendUrl) + "/api/facebook-campaign-resumptions";
    this.metaApiUrl = trimSlash(metaUrl) + "/" + apiVersion;
    this.configuration = configuration;
    this.json = json;
    this.apiVersion = apiVersion;
  }

  /** Consome somente a fila canônica e reserva cada autorização antes de executar. */
  @Scheduled(cron = "0 * * * * *")
  public void poll() {
    try {
      LOG.info(
          "Retomada backend GET: url==>{}/pending params={}",
          backendApiUrl,
          JsonLogFormatter.wrap(json, Map.of()));
      JsonNode pending =
          backend
              .get()
              .uri("/pending")
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block(Duration.ofSeconds(30));
      LOG.info(
          "Retomada backend GET: url<=={}/pending response={}",
          backendApiUrl,
          JsonLogFormatter.wrap(json, pending));
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
          LOG.info(
              "Retomada backend POST: url==>{}/{}/claim payload={}",
              backendApiUrl,
              id,
              JsonLogFormatter.wrap(json, Map.of()));
          JsonNode claimed =
              backend
                  .post()
                  .uri("/" + id + "/claim")
                  .retrieve()
                  .bodyToMono(JsonNode.class)
                  .block(Duration.ofSeconds(30));
          LOG.info(
              "Retomada backend POST: url<=={}/{}/claim response={}",
              backendApiUrl,
              id,
              JsonLogFormatter.wrap(json, claimed));
          execute(claimed, config.accessToken());
        } catch (Exception ex) {
          LOG.error(
              "Falha consumindo retomada: requestId={} url<=={}/{}/claim",
              id,
              backendApiUrl,
              id,
              ex);
        }
      }
    } catch (Exception ex) {
      LOG.error(
          "Falha consultando fila de retomadas Facebook: url<=={}/pending", backendApiUrl, ex);
    }
  }

  /** Aplica a autorização idempotente sem enviar limites nulos e compensa falhas com pausa. */
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
              "id,status,effective_status,spend_cap,daily_budget,lifetime_budget,can_use_spend_cap,account_id,adsets.limit(2){id,status,effective_status,lifetime_budget,lifetime_spend_cap,daily_budget,daily_spend_cap,end_time}",
              token,
              requestId);
      evidence.set("before", before);
      String accountId = before.path("account_id").asText();
      if (!accountId.matches("[0-9]+"))
        throw new IllegalStateException("Conta Meta não confirmada");
      JsonNode account =
          get("act_" + accountId, "currency,min_campaign_group_spend_cap", token, requestId);
      evidence.set("account", account);
      JsonNode sets = before.path("adsets").path("data");
      if (!"BRL".equals(account.path("currency").asText())
          || !sets.isArray()
          || sets.size() != 1
          || !adSetId.equals(sets.get(0).path("id").asText()))
        throw new IllegalStateException("Retomada exige moeda BRL e um único conjunto autorizado");
      JsonNode beforeAdSet = sets.get(0);
      long campaignDailyBudget = before.path("daily_budget").asLong(0L);
      long campaignLifetimeBudget = before.path("lifetime_budget").asLong(0L);
      boolean lifetimeMode =
          campaignDailyBudget == 0L
              && campaignLifetimeBudget == 0L
              && beforeAdSet.path("lifetime_budget").asLong() > 0
              && beforeAdSet.path("daily_budget").asLong() == 0;
      boolean dailyMode =
          campaignDailyBudget == 0L
              && campaignLifetimeBudget == 0L
              && beforeAdSet.path("daily_budget").asLong() > 0
              && beforeAdSet.path("lifetime_budget").asLong() == 0;
      boolean campaignDailyMode =
          campaignDailyBudget == dailyMinor
              && campaignLifetimeBudget == 0L
              && beforeAdSet.path("lifetime_budget").asLong() == 0L;
      boolean campaignLifetimeMode =
          campaignDailyBudget == 0L
              && campaignLifetimeBudget > 0L
              && beforeAdSet.path("daily_budget").asLong() == 0L
              && beforeAdSet.path("lifetime_budget").asLong() == 0L;
      if (!lifetimeMode && !dailyMode && !campaignDailyMode && !campaignLifetimeMode)
        throw new IllegalStateException(
            "Modo de orçamento do conjunto não permite retomada segura");
      if (!"PAUSED".equals(before.path("status").asText())
          && !"ACTIVE".equals(before.path("status").asText()))
        throw new IllegalStateException("Estado da campanha não permite retomada");
      if ("ACTIVE".equals(before.path("status").asText())) {
        post(campaignId, Map.of("status", "PAUSED"), token, requestId);
      }
      JsonNode insights = insights(campaignId, token, requestId);
      evidence.set("insights", insights);
      JsonNode rows = insights.path("data");
      if (!rows.isArray() || rows.size() != 1 || !rows.get(0).hasNonNull("spend"))
        throw new IllegalStateException("Gasto acumulado não confirmado pela Meta");
      BigDecimal spend = new BigDecimal(rows.get(0).path("spend").asText());
      if (spend.compareTo(cap) >= 0)
        throw new IllegalStateException("Teto autorizado já consumido");
      long spentMinor =
          spend.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
      verifyDestination(task.path("destinationUrl").asText(), requestId);
      String budgetMode;
      long nativeCampaignLifetimeBudgetMinor = 0L;
      long remainingDays = 0L;
      long effectiveRemainingAverageMinor = 0L;
      long minimumCampaignSpendCap = account.path("min_campaign_group_spend_cap").asLong(0L);
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
        boolean belowCampaignMinimum =
            minimumCampaignSpendCap > 0 && minor < minimumCampaignSpendCap;
        boolean campaignCapSupported =
            dailyMode
                && before.path("can_use_spend_cap").asBoolean(false)
                && !belowCampaignMinimum
                && beforeAdSet.path("lifetime_spend_cap").asLong(0L) == 0L;
        if (campaignCapSupported) {
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
        } else if (
            belowCampaignMinimum && (dailyMode || campaignDailyMode || campaignLifetimeMode)) {
          if (before.path("spend_cap").asLong(0L) > 0L)
            throw new IllegalStateException(
                "A campanha possui spend_cap anterior; remova-o pelo contrato oficial antes da"
                    + " migração");
          budgetMode = "CAMPAIGN_LIFETIME_BELOW_MINIMUM";
          remainingDays = ChronoUnit.DAYS.between(today, endDate) + 1L;
          long dailyWindowLimitMinor = Math.multiplyExact(dailyMinor, remainingDays);
          nativeCampaignLifetimeBudgetMinor =
              Math.min(minor, Math.addExact(spentMinor, dailyWindowLimitMinor));
          if (nativeCampaignLifetimeBudgetMinor <= spentMinor)
            throw new IllegalStateException(
                "Janela restante não comporta orçamento nativo acima do gasto confirmado");
          effectiveRemainingAverageMinor =
              (nativeCampaignLifetimeBudgetMinor - spentMinor + remainingDays - 1L)
                  / remainingDays;
          post(
              campaignId,
              Map.of(
                  "lifetime_budget",
                  Long.toString(nativeCampaignLifetimeBudgetMinor),
                  "stop_time",
                  end.toString(),
                  "status",
                  "PAUSED"),
              token,
              requestId);
          JsonNode migratedCampaign =
              get(
                  campaignId,
                  "id,status,effective_status,spend_cap,daily_budget,lifetime_budget,stop_time",
                  token,
                  requestId);
          evidence.set("migratedCampaign", migratedCampaign);
          if (migratedCampaign.path("daily_budget").asLong(0L) != 0L
              || migratedCampaign.path("lifetime_budget").asLong(-1L)
                  != nativeCampaignLifetimeBudgetMinor
              || !parseMetaInstant(migratedCampaign.path("stop_time").asText()).equals(end))
            throw new IllegalStateException(
                "Meta não confirmou a migração para orçamento vitalício da campanha");
          JsonNode migratedAdSet =
              get(
                  adSetId,
                  "id,status,effective_status,lifetime_budget,lifetime_spend_cap,daily_budget,daily_spend_cap,end_time",
                  token,
                  requestId);
          evidence.set("migratedAdSet", migratedAdSet);
          if (migratedAdSet.path("daily_budget").asLong(0L) != 0L
              || migratedAdSet.path("lifetime_budget").asLong(0L) != 0L
              || migratedAdSet.path("daily_spend_cap").asLong(0L) != 0L
              || migratedAdSet.path("lifetime_spend_cap").asLong(0L) != 0L)
            throw new IllegalStateException(
                "Meta não removeu orçamento e limites próprios do conjunto após a migração");
          post(
              adSetId,
              Map.of(
                  "end_time",
                  end.toString(),
                  "status",
                  "ACTIVE"),
              token,
              requestId);
        } else {
          throw new IllegalStateException(
              "A conta Meta não confirmou um teto nativo compatível com a autorização");
        }
      }
      JsonNode verified =
          get(
              adSetId,
              "id,status,effective_status,lifetime_budget,lifetime_spend_cap,daily_budget,daily_spend_cap,end_time",
              token,
              requestId);
      evidence.set("verifiedAdSet", verified);
      if (!"ACTIVE".equals(verified.path("status").asText())
          || !parseMetaInstant(verified.path("end_time").asText()).equals(end))
        throw new IllegalStateException(
            "Meta não confirmou estado e prazo autorizados do conjunto");
      JsonNode verifiedCampaign =
          get(
              campaignId,
              "id,status,spend_cap,daily_budget,lifetime_budget,stop_time",
              token,
              requestId);
      evidence.set("verifiedCampaign", verifiedCampaign);
      if (lifetimeMode
          && (verified.path("lifetime_budget").asLong(-1) != minor
              || verified.path("daily_budget").asLong() != 0))
        throw new IllegalStateException("Meta não confirmou orçamento vitalício autorizado");
      if (dailyMode || campaignDailyMode || campaignLifetimeMode) {
        boolean campaignLifetimeFallback =
            "CAMPAIGN_LIFETIME_BELOW_MINIMUM".equals(budgetMode);
        boolean dailyBudgetConfirmed =
            campaignLifetimeFallback
                ? verifiedCampaign.path("daily_budget").asLong() == 0L
                    && verifiedCampaign.path("lifetime_budget").asLong(-1L)
                        == nativeCampaignLifetimeBudgetMinor
                    && verifiedCampaign.path("spend_cap").asLong() == 0L
                    && verified.path("daily_budget").asLong() == 0L
                    && verified.path("lifetime_budget").asLong() == 0L
                    && verified.path("daily_spend_cap").asLong() == 0L
                    && verified.path("lifetime_spend_cap").asLong() == 0L
                    && parseMetaInstant(verifiedCampaign.path("stop_time").asText()).equals(end)
                : verified.path("daily_budget").asLong(-1) == dailyMinor
                    && verified.path("lifetime_budget").asLong() == 0L;
        boolean nativeCapConfirmed =
            "DAILY_WITH_CAMPAIGN_CAP".equals(budgetMode)
                ? verifiedCampaign.path("spend_cap").asLong(-1) == minor
                : verifiedCampaign.path("lifetime_budget").asLong(-1L)
                        == nativeCampaignLifetimeBudgetMinor
                    && nativeCampaignLifetimeBudgetMinor <= minor
                    && effectiveRemainingAverageMinor <= dailyMinor;
        if (!dailyBudgetConfirmed || !nativeCapConfirmed)
          throw new IllegalStateException("Meta não confirmou orçamento e teto acumulado");
      }
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
      evidence.put("accountMinimumCampaignSpendCapMinor", minimumCampaignSpendCap);
      evidence.put("campaignSpendCapMinor", verifiedCampaign.path("spend_cap").asLong());
      evidence.put("dailyBudgetMinor", dailyMinor);
      evidence.put("authorizedDailyBudgetMinor", dailyMinor);
      evidence.put("campaignDailyBudgetMinor", verifiedCampaign.path("daily_budget").asLong());
      evidence.put(
          "campaignLifetimeBudgetMinor", verifiedCampaign.path("lifetime_budget").asLong());
      evidence.put("adSetDailyBudgetMinor", verified.path("daily_budget").asLong());
      evidence.put("lifetimeBudgetMinor", verified.path("lifetime_budget").asLong());
      evidence.put("adSetLifetimeSpendCapMinor", verified.path("lifetime_spend_cap").asLong());
      evidence.put("remainingDays", remainingDays);
      evidence.put("effectiveRemainingAverageMinor", effectiveRemainingAverageMinor);
      evidence.put("startDate", startDate.toString());
      evidence.put("endDate", endDate.toString());
      evidence.put("spend", spend);
      result(task, true, null, evidence);
    } catch (Exception ex) {
      LOG.error("Falha retomando campanha: requestId={} campaignId={}", requestId, campaignId, ex);
      addFailureEvidence(evidence, ex);
      try {
        ensureCampaignPaused(campaignId, token, requestId);
        evidence.put("compensation", "PAUSED");
      } catch (Exception pauseEx) {
        LOG.error(
            "Falha na pausa compensatória: requestId={} campaignId={} url<=={}/{}",
            requestId,
            campaignId,
            metaApiUrl,
            campaignId,
            pauseEx);
        evidence.put("compensation", "PAUSE_UNCONFIRMED");
      }
      try {
        result(task, false, ex.getMessage(), evidence);
      } catch (Exception callbackEx) {
        LOG.error(
            "Falha registrando resultado: requestId={} campaignId={} url<=={}/{}/result",
            requestId,
            campaignId,
            backendApiUrl,
            requestId,
            callbackEx);
      }
    }
  }

  /** Consulta evidência nativa com autenticação em header e auditoria sem token. */
  private JsonNode get(String id, String fields, String token, long requestId) {
    String endpoint = metaApiUrl + "/" + id;
    LOG.info(
        "Retomada Meta GET: requestId={} url==>{} params={}",
        requestId,
        endpoint,
        JsonLogFormatter.wrap(json, Map.of("fields", fields)));
    try {
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
          "Retomada Meta GET: requestId={} url<=={} response={}",
          requestId,
          endpoint,
          JsonLogFormatter.wrap(json, response));
      return response;
    } catch (WebClientResponseException ex) {
      throw metaFailure("GET", endpoint, null, ex, requestId);
    }
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
    String endpoint = metaApiUrl + "/" + campaignId + "/insights";
    LOG.info(
        "Retomada Meta GET: requestId={} url==>{} params={}",
        requestId,
        endpoint,
        JsonLogFormatter.wrap(
            json, Map.of("fields", "spend", "time_range", range, "time_increment", "all_days")));
    try {
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
          "Retomada Meta GET: requestId={} url<=={} response={}",
          requestId,
          endpoint,
          JsonLogFormatter.wrap(json, response));
      return response;
    } catch (WebClientResponseException ex) {
      throw metaFailure("GET", endpoint, null, ex, requestId);
    }
  }

  /** Escreve parâmetros autorizados e exige confirmação explícita da Meta. */
  private void post(String id, Map<String, String> body, String token, long requestId) {
    String endpoint = metaApiUrl + "/" + id;
    LOG.info(
        "Retomada Meta POST: requestId={} url==>{} payload={}",
        requestId,
        endpoint,
        JsonLogFormatter.wrap(json, body));
    try {
      JsonNode response =
          meta.post()
              .uri("/" + apiVersion + "/" + id)
              .headers(h -> h.setBearerAuth(token))
              .bodyValue(body)
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block(Duration.ofSeconds(30));
      LOG.info(
          "Retomada Meta POST: requestId={} url<=={} response={}",
          requestId,
          endpoint,
          JsonLogFormatter.wrap(json, response));
      if (response == null || !response.path("success").asBoolean())
        throw new IllegalStateException("Meta não confirmou atualização");
    } catch (WebClientResponseException ex) {
      throw metaFailure("POST", endpoint, body, ex, requestId);
    }
  }

  /** Confirma a pausa sem repetir uma escrita quando a campanha já está protegida. */
  private void ensureCampaignPaused(String campaignId, String token, long requestId) {
    if (campaignId == null || campaignId.isBlank())
      throw new IllegalStateException("Campanha ausente para compensação");
    JsonNode current = get(campaignId, "id,status,effective_status", token, requestId);
    if (!"PAUSED".equals(current.path("status").asText())) {
      post(campaignId, Map.of("status", "PAUSED"), token, requestId);
      current = get(campaignId, "id,status,effective_status", token, requestId);
    }
    if (!"PAUSED".equals(current.path("status").asText()))
      throw new IllegalStateException("Meta não confirmou a pausa compensatória");
  }

  /** Preserva status, endpoint e corpo oficial da falha Meta sem expor a credencial. */
  private MetaRequestException metaFailure(
      String method,
      String endpoint,
      Object request,
      WebClientResponseException ex,
      long requestId) {
    String responseBody = ex.getResponseBodyAsString();
    LOG.error(
        "Retomada Meta falhou: requestId={} method={} url<=={} status={} payload={} response={}",
        requestId,
        method,
        endpoint,
        ex.getStatusCode().value(),
        JsonLogFormatter.wrap(json, request),
        responseBody,
        ex);
    String detail = responseBody;
    try {
      JsonNode error = json.readTree(responseBody).path("error");
      if (!error.path("message").asText().isBlank()) {
        detail =
            error.path("message").asText()
                + " (code="
                + error.path("code").asText("unknown")
                + ", subcode="
                + error.path("error_subcode").asText("unknown")
                + ")";
      }
    } catch (Exception parseEx) {
      LOG.warn(
          "Resposta de erro Meta não é JSON: requestId={} method={} endpoint={}",
          requestId,
          method,
          endpoint,
          parseEx);
    }
    return new MetaRequestException(
        method, endpoint, ex.getStatusCode().value(), responseBody, detail, ex);
  }

  /** Acrescenta o erro estruturado à auditoria devolvida ao backend. */
  private void addFailureEvidence(ObjectNode evidence, Exception ex) {
    if (!(ex instanceof MetaRequestException failure)) return;
    ObjectNode error = evidence.putObject("metaError");
    error.put("method", failure.method());
    error.put("endpoint", failure.endpoint());
    error.put("httpStatus", failure.httpStatus());
    try {
      error.set("response", json.readTree(failure.responseBody()));
    } catch (Exception parseEx) {
      LOG.warn(
          "Falha ao estruturar erro Meta para auditoria: endpoint={}", failure.endpoint(), parseEx);
      error.put("response", failure.responseBody());
    }
  }

  /** Interpreta as duas formas de data devolvidas pela Graph API. */
  private Instant parseMetaInstant(String value) {
    return value.endsWith("Z")
        ? Instant.parse(value)
        : OffsetDateTime.parse(
                value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX"))
            .toInstant();
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
    String endpoint = backendApiUrl + "/" + task.path("id").asLong() + "/result";
    LOG.info(
        "Retomada backend POST: url==>{} payload={}", endpoint, JsonLogFormatter.wrap(json, body));
    backend
        .post()
        .uri("/" + task.path("id").asLong() + "/result")
        .bodyValue(body)
        .retrieve()
        .toBodilessEntity()
        .block(Duration.ofSeconds(30));
    LOG.info(
        "Retomada backend POST: url<=={} response={}",
        endpoint,
        JsonLogFormatter.wrap(json, Map.of()));
  }

  /** Remove a barra final para compor URLs de auditoria sem alterar o cliente HTTP. */
  private static String trimSlash(String value) {
    return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  /** Responsabilidade: transportar uma falha HTTP Meta com evidência segura para o callback. */
  private static final class MetaRequestException extends RuntimeException {
    private final String method;
    private final String endpoint;
    private final int httpStatus;
    private final String responseBody;

    /** Preserva contrato HTTP, resposta e stack trace da integração original. */
    private MetaRequestException(
        String method,
        String endpoint,
        int httpStatus,
        String responseBody,
        String detail,
        Throwable cause) {
      super(
          "Meta " + method + " " + endpoint + " respondeu HTTP " + httpStatus + ": " + detail,
          cause);
      this.method = method;
      this.endpoint = endpoint;
      this.httpStatus = httpStatus;
      this.responseBody = responseBody;
    }

    /** Devolve o método HTTP que falhou. */
    private String method() {
      return method;
    }

    /** Devolve o endpoint sem credencial. */
    private String endpoint() {
      return endpoint;
    }

    /** Devolve o status oficial da Graph API. */
    private int httpStatus() {
      return httpStatus;
    }

    /** Devolve o corpo oficial para evidência estruturada. */
    private String responseBody() {
      return responseBody;
    }
  }
}
