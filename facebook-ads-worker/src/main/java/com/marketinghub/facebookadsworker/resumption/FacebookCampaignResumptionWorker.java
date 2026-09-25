package com.marketinghub.facebookadsworker.resumption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.facebookadsworker.configuration.FacebookWorkerConfigurationClient;
import com.marketinghub.facebookadsworker.util.JsonLogFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Executa retomadas autorizadas e confirma orçamento nativo antes de ativar a hierarquia vigente.
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

  /** Aplica a autorização idempotente sem ampliar limites e compensa falhas com pausa. */
  public void execute(JsonNode task, String token) {
    String sourceCampaignId = task.path("campaignId").asText();
    AtomicReference<String> effectiveCampaignId = new AtomicReference<>(sourceCampaignId);
    long requestId = task.path("id").asLong();
    ObjectNode evidence = json.createObjectNode();
    try {
      String sourceAdSetId = task.path("adSetId").asText();
      BigDecimal cap = task.path("totalLimit").decimalValue();
      BigDecimal dailyBudget = task.path("dailyBudget").decimalValue();
      if (!task.hasNonNull("historicalSpend") || !task.path("historicalSpend").isNumber())
        throw new IllegalStateException("Gasto histórico agregado não informado pelo backend");
      BigDecimal historicalSpend = task.path("historicalSpend").decimalValue();
      LocalDate startDate = LocalDate.parse(task.path("startDate").asText());
      LocalDate endDate = LocalDate.parse(task.path("endDate").asText());
      Instant end = endDate.atTime(23, 59, 59).atZone(ZoneId.of("America/Sao_Paulo")).toInstant();
      LocalDate today = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
      if (today.isBefore(startDate)
          || today.isAfter(endDate)
          || cap.signum() <= 0
          || dailyBudget.signum() <= 0
          || historicalSpend.signum() < 0
          || dailyBudget.compareTo(cap) > 0)
        throw new IllegalStateException("Autorização financeira fora da janela ou inválida");
      long totalMinor = cap.movePointRight(2).longValueExact();
      long dailyMinor = dailyBudget.movePointRight(2).longValueExact();
      long historicalMinor =
          historicalSpend
              .movePointRight(2)
              .setScale(0, RoundingMode.UNNECESSARY)
              .longValueExact();
      JsonNode before =
          get(
              sourceCampaignId,
              "id,name,objective,special_ad_categories,special_ad_category_country,status,effective_status,spend_cap,daily_budget,lifetime_budget,bid_strategy,can_use_spend_cap,account_id,adsets.limit(2){id,name,status,effective_status,lifetime_budget,lifetime_spend_cap,daily_budget,daily_spend_cap,end_time,billing_event,optimization_goal,destination_type,bid_strategy,bid_amount,targeting,promoted_object,attribution_spec,ads.limit(100){id,name,status,effective_status,creative{id}}}",
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
          || !sourceAdSetId.equals(sets.get(0).path("id").asText()))
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
        post(sourceCampaignId, Map.of("status", "PAUSED"), token, requestId);
      }
      JsonNode insights = insights(sourceCampaignId, token, requestId);
      evidence.set("insights", insights);
      JsonNode rows = insights.path("data");
      if (!rows.isArray() || rows.size() != 1 || !rows.get(0).hasNonNull("spend"))
        throw new IllegalStateException("Gasto acumulado não confirmado pela Meta");
      BigDecimal sourceSpend = new BigDecimal(rows.get(0).path("spend").asText());
      BigDecimal confirmedPriorSpend =
          historicalSpend.add(sourceSpend).setScale(2, RoundingMode.HALF_UP);
      if (confirmedPriorSpend.compareTo(cap) >= 0)
        throw new IllegalStateException("Teto autorizado já consumido");
      long sourceSpentMinor =
          sourceSpend.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
      long confirmedPriorSpendMinor =
          confirmedPriorSpend
              .movePointRight(2)
              .setScale(0, RoundingMode.HALF_UP)
              .longValueExact();
      long sourceLimitMinor = Math.subtractExact(totalMinor, historicalMinor);
      if (sourceLimitMinor <= sourceSpentMinor)
        throw new IllegalStateException("Saldo nativo da campanha vigente já foi consumido");
      verifyDestination(task.path("destinationUrl").asText(), requestId);
      String budgetMode;
      long nativeCampaignLifetimeBudgetMinor = 0L;
      long remainingDays = 0L;
      long effectiveRemainingAverageMinor = 0L;
      long minimumCampaignSpendCap = account.path("min_campaign_group_spend_cap").asLong(0L);
      ReplacementExecution replacement = null;
      if (lifetimeMode) {
        budgetMode = "LIFETIME";
        if (before.path("spend_cap").asLong() > 0)
          throw new IllegalStateException(
              "Campanha vitalícia possui spend_cap concorrente e não pode ser retomada");
        post(
            sourceAdSetId,
            Map.of(
                "lifetime_budget",
                Long.toString(sourceLimitMinor),
                "end_time",
                end.toString(),
                "status",
                "ACTIVE"),
            token,
            requestId);
      } else {
        boolean belowCampaignMinimum =
            minimumCampaignSpendCap > 0 && sourceLimitMinor < minimumCampaignSpendCap;
        boolean campaignCapSupported =
            dailyMode
                && before.path("can_use_spend_cap").asBoolean(false)
                && !belowCampaignMinimum
                && beforeAdSet.path("lifetime_spend_cap").asLong(0L) == 0L;
        if (campaignCapSupported) {
          budgetMode = "DAILY_WITH_CAMPAIGN_CAP";
          post(
              sourceCampaignId,
              Map.of("spend_cap", Long.toString(sourceLimitMinor)),
              token,
              requestId);
          post(
              sourceAdSetId,
              Map.of(
                  "daily_budget",
                  Long.toString(dailyMinor),
                  "end_time",
                  end.toString(),
                  "status",
                  "ACTIVE"),
              token,
              requestId);
        } else if (belowCampaignMinimum && (dailyMode || campaignDailyMode)) {
          if (before.path("spend_cap").asLong(0L) > 0L)
            throw new IllegalStateException(
                "A campanha possui spend_cap anterior incompatível com a substituição segura");
          budgetMode = "REPLACEMENT_ADSET_LIFETIME_BELOW_MINIMUM";
          remainingDays = ChronoUnit.DAYS.between(today, endDate) + 1L;
          long dailyWindowLimitMinor = Math.multiplyExact(dailyMinor, remainingDays);
          long replacementLifetimeBudgetMinor =
              Math.min(
                  Math.subtractExact(totalMinor, confirmedPriorSpendMinor),
                  dailyWindowLimitMinor);
          if (replacementLifetimeBudgetMinor <= 0L)
            throw new IllegalStateException(
                "Janela restante não comporta saldo nativo para uma campanha substituta");
          effectiveRemainingAverageMinor =
              (replacementLifetimeBudgetMinor + remainingDays - 1L) / remainingDays;
          replacement =
              replaceCampaign(
                  before,
                  beforeAdSet,
                  sourceCampaignId,
                  sourceAdSetId,
                  accountId,
                  requestId,
                  replacementLifetimeBudgetMinor,
                  confirmedPriorSpend,
                  end,
                  token,
                  evidence,
                  effectiveCampaignId::set);
        } else if (belowCampaignMinimum && campaignLifetimeMode) {
          budgetMode = "CAMPAIGN_LIFETIME_BELOW_MINIMUM";
          remainingDays = ChronoUnit.DAYS.between(today, endDate) + 1L;
          long dailyWindowLimitMinor = Math.multiplyExact(dailyMinor, remainingDays);
          nativeCampaignLifetimeBudgetMinor =
              Math.min(sourceLimitMinor, Math.addExact(sourceSpentMinor, dailyWindowLimitMinor));
          if (nativeCampaignLifetimeBudgetMinor <= sourceSpentMinor)
            throw new IllegalStateException(
                "Janela restante não comporta orçamento nativo acima do gasto confirmado");
          effectiveRemainingAverageMinor =
              (nativeCampaignLifetimeBudgetMinor - sourceSpentMinor + remainingDays - 1L)
                  / remainingDays;
          post(
              sourceCampaignId,
              Map.of(
                  "lifetime_budget",
                  Long.toString(nativeCampaignLifetimeBudgetMinor),
                  "stop_time",
                  end.toString(),
                  "status",
                  "PAUSED"),
              token,
              requestId);
          post(
              sourceAdSetId,
              Map.of("end_time", end.toString(), "status", "ACTIVE"),
              token,
              requestId);
        } else {
          throw new IllegalStateException(
              "A conta Meta não confirmou um teto nativo compatível com a autorização");
        }
      }
      JsonNode verified;
      JsonNode verifiedCampaign;
      if (replacement != null) {
        verified = replacement.verifiedAdSet();
        verifiedCampaign = replacement.verifiedCampaign();
      } else {
        verified =
            get(
                sourceAdSetId,
                "id,status,effective_status,lifetime_budget,lifetime_spend_cap,daily_budget,daily_spend_cap,end_time",
                token,
                requestId);
        evidence.set("verifiedAdSet", verified);
        if (!"ACTIVE".equals(verified.path("status").asText())
            || !parseMetaInstant(verified.path("end_time").asText()).equals(end))
          throw new IllegalStateException(
              "Meta não confirmou estado e prazo autorizados do conjunto");
        verifiedCampaign =
            get(
                sourceCampaignId,
                "id,status,spend_cap,daily_budget,lifetime_budget,stop_time",
                token,
                requestId);
        evidence.set("verifiedCampaign", verifiedCampaign);
      }
      if (lifetimeMode
          && (verified.path("lifetime_budget").asLong(-1) != sourceLimitMinor
              || verified.path("daily_budget").asLong() != 0))
        throw new IllegalStateException("Meta não confirmou orçamento vitalício autorizado");
      if (replacement == null && (dailyMode || campaignDailyMode || campaignLifetimeMode)) {
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
                ? verifiedCampaign.path("spend_cap").asLong(-1) == sourceLimitMinor
                : verifiedCampaign.path("lifetime_budget").asLong(-1L)
                        == nativeCampaignLifetimeBudgetMinor
                    && nativeCampaignLifetimeBudgetMinor <= sourceLimitMinor
                    && effectiveRemainingAverageMinor <= dailyMinor;
        if (!dailyBudgetConfirmed || !nativeCapConfirmed)
          throw new IllegalStateException("Meta não confirmou orçamento e teto acumulado");
      }
      if (replacement == null) {
        post(sourceCampaignId, Map.of("status", "ACTIVE"), token, requestId);
      }
      JsonNode active =
          get(effectiveCampaignId.get(), "id,status,effective_status", token, requestId);
      evidence.set("after", active);
      if (!"ACTIVE".equals(active.path("status").asText())
          || !"ACTIVE".equals(active.path("effective_status").asText()))
        throw new IllegalStateException("Meta ainda não confirmou campanha ativa");
      evidence.put("campaignId", effectiveCampaignId.get());
      evidence.put("campaignStatus", "ACTIVE");
      evidence.put(
          "adSetId", replacement == null ? sourceAdSetId : replacement.adSetId());
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
      evidence.put("historicalSpendMinor", historicalMinor);
      evidence.put("sourceCampaignSpend", sourceSpend);
      evidence.put("confirmedPriorSpendMinor", confirmedPriorSpendMinor);
      evidence.put("startDate", startDate.toString());
      evidence.put("endDate", endDate.toString());
      evidence.put("spend", confirmedPriorSpend);
      result(task, true, null, evidence, replacement == null ? null : replacement.callback());
    } catch (Exception ex) {
      LOG.error(
          "Falha retomando campanha: requestId={} sourceCampaignId={} effectiveCampaignId={}",
          requestId,
          sourceCampaignId,
          effectiveCampaignId.get(),
          ex);
      addFailureEvidence(evidence, ex);
      if (callbackCompleted(task)) {
        LOG.info(
            "Callback de retomada já persistido; preservando campanha ativa: requestId={} campaignId={}",
            requestId,
            effectiveCampaignId.get());
        return;
      }
      try {
        ensureCampaignPaused(effectiveCampaignId.get(), token, requestId);
        ensureCampaignPaused(sourceCampaignId, token, requestId);
        evidence.put("compensation", "PAUSED");
      } catch (Exception pauseEx) {
        LOG.error(
            "Falha na pausa compensatória: requestId={} sourceCampaignId={} effectiveCampaignId={} url<=={}/{}",
            requestId,
            sourceCampaignId,
            effectiveCampaignId.get(),
            metaApiUrl,
            effectiveCampaignId.get(),
            pauseEx);
        evidence.put("compensation", "PAUSE_UNCONFIRMED");
      }
      try {
        result(task, false, ex.getMessage(), evidence, null);
      } catch (Exception callbackEx) {
        LOG.error(
            "Falha registrando resultado: requestId={} sourceCampaignId={} url<=={}/{}/result",
            requestId,
            sourceCampaignId,
            backendApiUrl,
            requestId,
            callbackEx);
      }
    }
  }

  /**
   * Cria ou recupera uma hierarquia determinística com orçamento vitalício apenas sobre o saldo
   * restante, sem alterar o tipo de orçamento da campanha anterior.
   */
  private ReplacementExecution replaceCampaign(
      JsonNode sourceCampaign,
      JsonNode sourceAdSet,
      String sourceCampaignId,
      String sourceAdSetId,
      String accountId,
      long requestId,
      long lifetimeBudgetMinor,
      BigDecimal confirmedPriorSpend,
      Instant end,
      String token,
      ObjectNode evidence,
      java.util.function.Consumer<String> registerEffectiveCampaign) {
    JsonNode sourceAds = sourceAdSet.path("ads").path("data");
    if (sourceCampaign.path("name").asText().isBlank()
        || sourceCampaign.path("objective").asText().isBlank()
        || sourceAdSet.path("name").asText().isBlank()
        || sourceAdSet.path("billing_event").asText().isBlank()
        || sourceAdSet.path("optimization_goal").asText().isBlank()
        || !sourceAdSet.path("targeting").isObject()
        || !sourceAds.isArray()
        || sourceAds.isEmpty()) {
      throw new IllegalStateException("Configuração da hierarquia de origem está incompleta");
    }
    String replacementBidStrategy = sourceAdSet.path("bid_strategy").asText();
    if (replacementBidStrategy.isBlank())
      replacementBidStrategy = sourceCampaign.path("bid_strategy").asText();
    if (replacementBidStrategy.isBlank())
      throw new IllegalStateException("Estratégia de lance da hierarquia de origem não confirmada");
    long replacementBidAmount = sourceAdSet.path("bid_amount").asLong(0L);
    if (!"LOWEST_COST_WITHOUT_CAP".equals(replacementBidStrategy)
        && replacementBidAmount <= 0L)
      throw new IllegalStateException(
          "Estratégia de lance limitada não possui valor de lance confirmado");
    final String confirmedBidStrategy = replacementBidStrategy;
    final long confirmedBidAmount = replacementBidAmount;
    String campaignName = replacementName(sourceCampaign.path("name").asText(), requestId);
    JsonNode targetCampaign =
        findByName(
                "act_" + accountId,
                "campaigns",
                "id,name,status,effective_status,objective,spend_cap,daily_budget,lifetime_budget",
                campaignName,
                token,
                requestId)
            .orElseGet(
                () -> {
                  Map<String, Object> body = new LinkedHashMap<>();
                  body.put("name", campaignName);
                  body.put("objective", sourceCampaign.path("objective").asText());
                  body.put("status", "PAUSED");
                  body.put(
                      "special_ad_categories",
                      sourceCampaign.path("special_ad_categories").isArray()
                          ? sourceCampaign.path("special_ad_categories")
                          : json.createArrayNode());
                  if (sourceCampaign.path("special_ad_category_country").isArray()
                      && !sourceCampaign.path("special_ad_category_country").isEmpty()) {
                    body.put(
                        "special_ad_category_country",
                        sourceCampaign.path("special_ad_category_country"));
                  }
                  body.put("is_adset_budget_sharing_enabled", false);
                  String id = postForId("act_" + accountId + "/campaigns", body, token, requestId);
                  return get(
                      id,
                      "id,name,status,effective_status,objective,spend_cap,daily_budget,lifetime_budget",
                      token,
                      requestId);
                });
    String targetCampaignId = targetCampaign.path("id").asText();
    if (!targetCampaignId.matches("[0-9]+"))
      throw new IllegalStateException("Campanha substituta não recebeu identificador Meta");
    registerEffectiveCampaign.accept(targetCampaignId);
    ensureCampaignPaused(targetCampaignId, token, requestId);
    targetCampaign =
        get(
            targetCampaignId,
            "id,name,status,effective_status,objective,spend_cap,daily_budget,lifetime_budget",
            token,
            requestId);
    if (!campaignName.equals(targetCampaign.path("name").asText())
        || !sourceCampaign
            .path("objective")
            .asText()
            .equals(targetCampaign.path("objective").asText())
        || targetCampaign.path("spend_cap").asLong(0L) != 0L
        || targetCampaign.path("daily_budget").asLong(0L) != 0L
        || targetCampaign.path("lifetime_budget").asLong(0L) != 0L) {
      throw new IllegalStateException("Campanha substituta diverge do contrato sem orçamento CBO");
    }

    String adSetName = replacementName(sourceAdSet.path("name").asText(), requestId);
    JsonNode targetAdSet =
        findByName(
                targetCampaignId,
                "adsets",
                "id,name,status,effective_status,campaign_id,lifetime_budget,lifetime_spend_cap,daily_budget,daily_spend_cap,end_time,bid_strategy,bid_amount",
                adSetName,
                token,
                requestId)
            .orElseGet(
                () -> {
                  Map<String, Object> body = new LinkedHashMap<>();
                  body.put("name", adSetName);
                  body.put("campaign_id", targetCampaignId);
                  body.put("lifetime_budget", Long.toString(lifetimeBudgetMinor));
                  body.put("end_time", end.toString());
                  body.put("billing_event", sourceAdSet.path("billing_event").asText());
                  body.put("optimization_goal", sourceAdSet.path("optimization_goal").asText());
                  if (!sourceAdSet.path("destination_type").asText().isBlank())
                    body.put("destination_type", sourceAdSet.path("destination_type").asText());
                  body.put("targeting", sourceAdSet.path("targeting"));
                  if (sourceAdSet.path("promoted_object").isObject()
                      && !sourceAdSet.path("promoted_object").isEmpty())
                    body.put("promoted_object", sourceAdSet.path("promoted_object"));
                  if (sourceAdSet.path("attribution_spec").isArray()
                      && !sourceAdSet.path("attribution_spec").isEmpty())
                    body.put("attribution_spec", sourceAdSet.path("attribution_spec"));
                  body.put("bid_strategy", confirmedBidStrategy);
                  if (confirmedBidAmount > 0L
                      && !"LOWEST_COST_WITHOUT_CAP".equals(confirmedBidStrategy))
                    body.put("bid_amount", Long.toString(confirmedBidAmount));
                  body.put("status", "PAUSED");
                  String id = postForId("act_" + accountId + "/adsets", body, token, requestId);
                  return get(
                      id,
                      "id,name,status,effective_status,campaign_id,lifetime_budget,lifetime_spend_cap,daily_budget,daily_spend_cap,end_time,bid_strategy,bid_amount",
                      token,
                      requestId);
                });
    String targetAdSetId = targetAdSet.path("id").asText();
    verifyReplacementAdSet(
        targetAdSet,
        targetCampaignId,
        adSetName,
        lifetimeBudgetMinor,
        end,
        confirmedBidStrategy,
        confirmedBidAmount);

    Map<String, String> targetAds = new LinkedHashMap<>();
    ArrayNode verifiedAds = evidence.putArray("replacementAds");
    for (JsonNode sourceAd : sourceAds) {
      String sourceAdId = sourceAd.path("id").asText();
      String creativeId = sourceAd.path("creative").path("id").asText();
      if (!sourceAdId.matches("[0-9]+") || !creativeId.matches("[0-9]+"))
        throw new IllegalStateException("Anúncio de origem não possui criativo Meta reutilizável");
      String adName =
          replacementAdName(sourceAd.path("name").asText(), requestId, sourceAdId);
      JsonNode targetAd =
          findByName(
                  targetAdSetId,
                  "ads",
                  "id,name,status,effective_status,adset_id,creative{id}",
                  adName,
                  token,
                  requestId)
              .orElseGet(
                  () -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("name", adName);
                    body.put("adset_id", targetAdSetId);
                    body.put("creative", Map.of("creative_id", creativeId));
                    body.put("status", "ACTIVE");
                    String id = postForId("act_" + accountId + "/ads", body, token, requestId);
                    return get(
                        id,
                        "id,name,status,effective_status,adset_id,creative{id}",
                        token,
                        requestId);
                  });
      if (!creativeId.equals(targetAd.path("creative").path("id").asText())
          || !targetAdSetId.equals(targetAd.path("adset_id").asText()))
        throw new IllegalStateException("Anúncio substituto diverge do criativo aprovado");
      if (!"ACTIVE".equals(targetAd.path("status").asText())) {
        post(targetAd.path("id").asText(), Map.of("status", "ACTIVE"), token, requestId);
        targetAd =
            get(
                targetAd.path("id").asText(),
                "id,name,status,effective_status,adset_id,creative{id}",
                token,
                requestId);
      }
      if (!"ACTIVE".equals(targetAd.path("status").asText()))
        throw new IllegalStateException("Meta não confirmou o anúncio substituto como ativo");
      targetAds.put(sourceAdId, targetAd.path("id").asText());
      verifiedAds.add(targetAd);
    }

    post(targetAdSetId, Map.of("status", "ACTIVE"), token, requestId);
    targetAdSet =
        get(
            targetAdSetId,
            "id,name,status,effective_status,campaign_id,lifetime_budget,lifetime_spend_cap,daily_budget,daily_spend_cap,end_time,bid_strategy,bid_amount",
            token,
            requestId);
    verifyReplacementAdSet(
        targetAdSet,
        targetCampaignId,
        adSetName,
        lifetimeBudgetMinor,
        end,
        confirmedBidStrategy,
        confirmedBidAmount);
    if (!"ACTIVE".equals(targetAdSet.path("status").asText()))
      throw new IllegalStateException("Meta não confirmou o conjunto substituto como ativo");
    post(targetCampaignId, Map.of("status", "ACTIVE"), token, requestId);
    JsonNode activeCampaign =
        get(
            targetCampaignId,
            "id,name,status,effective_status,objective,spend_cap,daily_budget,lifetime_budget",
            token,
            requestId);
    if (!"ACTIVE".equals(activeCampaign.path("status").asText())
        || !"ACTIVE".equals(activeCampaign.path("effective_status").asText()))
      throw new IllegalStateException("Meta não confirmou a campanha substituta como ativa");
    JsonNode sourceAfter =
        get(sourceCampaignId, "id,status,effective_status", token, requestId);
    if (!"PAUSED".equals(sourceAfter.path("status").asText()))
      throw new IllegalStateException("A campanha anterior não permaneceu pausada");
    evidence.set("replacementCampaign", activeCampaign);
    evidence.set("verifiedAdSet", targetAdSet);
    evidence.set("sourceAfter", sourceAfter);

    ObjectNode callback = json.createObjectNode();
    callback.put("sourceCampaignId", sourceCampaignId);
    callback.put("sourceAdSetId", sourceAdSetId);
    callback.put("campaignId", targetCampaignId);
    callback.put("adSetId", targetAdSetId);
    callback.put("lifetimeBudgetMinor", lifetimeBudgetMinor);
    callback.put("confirmedPriorSpend", confirmedPriorSpend);
    ArrayNode callbackAds = callback.putArray("ads");
    targetAds.forEach(
        (sourceAdId, targetAdId) -> {
          ObjectNode item = callbackAds.addObject();
          item.put("sourceAdId", sourceAdId);
          item.put("adId", targetAdId);
        });
    return new ReplacementExecution(
        targetCampaignId,
        targetAdSetId,
        lifetimeBudgetMinor,
        targetAdSet,
        activeCampaign,
        callback);
  }

  /** Confere teto, prazo, lance e filiação do conjunto substituto sem inferir campos ausentes. */
  private void verifyReplacementAdSet(
      JsonNode adSet,
      String campaignId,
      String name,
      long lifetimeBudgetMinor,
      Instant end,
      String bidStrategy,
      long bidAmount) {
    if (!campaignId.equals(adSet.path("campaign_id").asText())
        || !name.equals(adSet.path("name").asText())
        || adSet.path("lifetime_budget").asLong(-1L) != lifetimeBudgetMinor
        || adSet.path("daily_budget").asLong(0L) != 0L
        || adSet.path("daily_spend_cap").asLong(0L) != 0L
        || adSet.path("lifetime_spend_cap").asLong(0L) != 0L
        || !bidStrategy.equals(adSet.path("bid_strategy").asText())
        || (bidAmount > 0L && adSet.path("bid_amount").asLong(-1L) != bidAmount)
        || !parseMetaInstant(adSet.path("end_time").asText()).equals(end))
      throw new IllegalStateException(
          "Meta não confirmou orçamento, prazo ou lance do conjunto substituto");
  }

  /** Recupera um objeto Meta por nome exato e bloqueia ambiguidade em retries. */
  private Optional<JsonNode> findByName(
      String parentId,
      String edge,
      String fields,
      String name,
      String token,
      long requestId) {
    JsonNode response = getEdge(parentId, edge, fields, token, requestId);
    List<JsonNode> matches = new ArrayList<>();
    response.path("data").forEach(item -> {
      if (name.equals(item.path("name").asText())) matches.add(item);
    });
    if (matches.size() > 1)
      throw new IllegalStateException("Retry encontrou mais de um objeto Meta com o mesmo nome");
    return matches.stream().findFirst();
  }

  /** Consulta aresta Meta limitada para recuperar criações determinísticas após falhas de rede. */
  private JsonNode getEdge(
      String parentId, String edge, String fields, String token, long requestId) {
    String endpoint = metaApiUrl + "/" + parentId + "/" + edge;
    LOG.info(
        "Retomada Meta GET: requestId={} url==>{} params={}",
        requestId,
        endpoint,
        JsonLogFormatter.wrap(json, Map.of("fields", fields, "limit", 500)));
    try {
      JsonNode response =
          meta.get()
              .uri(
                  b ->
                      b.path("/" + apiVersion + "/" + parentId + "/" + edge)
                          .queryParam("fields", "{fields}")
                          .queryParam("limit", 500)
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
      return response == null ? json.createObjectNode() : response;
    } catch (WebClientResponseException ex) {
      throw metaFailure("GET", endpoint, null, ex, requestId);
    }
  }

  /** Cria um objeto Meta e exige o identificador oficial antes de prosseguir. */
  private String postForId(
      String path, Map<String, ?> body, String token, long requestId) {
    String endpoint = metaApiUrl + "/" + path;
    LOG.info(
        "Retomada Meta POST: requestId={} url==>{} payload={}",
        requestId,
        endpoint,
        JsonLogFormatter.wrap(json, body));
    try {
      JsonNode response =
          meta.post()
              .uri("/" + apiVersion + "/" + path)
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
      String id = response == null ? "" : response.path("id").asText();
      if (!id.matches("[0-9]+"))
        throw new IllegalStateException("Meta não devolveu o identificador criado");
      return id;
    } catch (WebClientResponseException ex) {
      throw metaFailure("POST", endpoint, body, ex, requestId);
    }
  }

  /** Consulta o backend após erro de callback para não pausar uma retomada já confirmada. */
  private boolean callbackCompleted(JsonNode task) {
    long requestId = task.path("id").asLong();
    String endpoint = backendApiUrl + "/" + requestId;
    try {
      LOG.info(
          "Retomada backend GET: url==>{} params={}",
          endpoint,
          JsonLogFormatter.wrap(json, Map.of()));
      JsonNode response =
          backend
              .get()
              .uri("/" + requestId)
              .retrieve()
              .bodyToMono(JsonNode.class)
              .block(Duration.ofSeconds(30));
      LOG.info(
          "Retomada backend GET: url<=={} response={}",
          endpoint,
          JsonLogFormatter.wrap(json, response));
      return response != null && "COMPLETED".equals(response.path("status").asText());
    } catch (Exception ex) {
      LOG.error(
          "Falha desambiguando callback da retomada: requestId={} url<=={}",
          requestId,
          endpoint,
          ex);
      return false;
    }
  }

  /** Mantém os nomes de campanha e conjunto determinísticos e dentro do limite da Meta. */
  private String replacementName(String sourceName, long requestId) {
    String suffix = " · retomada " + requestId;
    String base = sourceName == null || sourceName.isBlank() ? "Campanha" : sourceName;
    return base.substring(0, Math.min(base.length(), 255 - suffix.length())) + suffix;
  }

  /** Diferencia anúncios substitutos mesmo quando os nomes originais se repetem. */
  private String replacementAdName(String sourceName, long requestId, String sourceAdId) {
    String suffix =
        " · retomada "
            + requestId
            + " · origem "
            + sourceAdId.substring(Math.max(0, sourceAdId.length() - 8));
    String base = sourceName == null || sourceName.isBlank() ? "Anúncio" : sourceName;
    return base.substring(0, Math.min(base.length(), 255 - suffix.length())) + suffix;
  }

  /** Agrupa a hierarquia substituta confirmada e o contrato enviado ao backend. */
  private record ReplacementExecution(
      String campaignId,
      String adSetId,
      long lifetimeBudgetMinor,
      JsonNode verifiedAdSet,
      JsonNode verifiedCampaign,
      ObjectNode callback) {}

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
  private void post(String id, Map<String, ?> body, String token, long requestId) {
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
  private void result(
      JsonNode task,
      boolean success,
      String error,
      ObjectNode evidence,
      ObjectNode replacement) {
    ObjectNode body = json.createObjectNode();
    body.put("leaseToken", task.path("leaseToken").asText());
    body.put("success", success);
    body.put("error", error);
    body.set("evidence", evidence);
    if (replacement != null) body.set("replacement", replacement);
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
