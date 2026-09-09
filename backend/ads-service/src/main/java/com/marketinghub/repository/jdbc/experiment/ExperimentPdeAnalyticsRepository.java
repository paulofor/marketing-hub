package com.marketinghub.repository.jdbc.experiment;

import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary;
import com.marketinghub.experiment.monitoring.pde.PdeAnalyticsSummary.*;
import com.marketinghub.experiment.monitoring.pde.PdeCommercialOutcomeSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lê métricas PDE persistidas com o mesmo recorte de produto, versão e atribuição em todas as
 * consultas.
 */
@Repository
public class ExperimentPdeAnalyticsRepository {
  private static final String SCOPE =
      """
      FROM pde_funnel_event
      WHERE product_slug = :product AND experience_version = :version
        AND (JSON_UNQUOTE(JSON_EXTRACT(metadata_json, '$.experimentId')) = :experimentId
          OR (COALESCE(JSON_UNQUOTE(JSON_EXTRACT(metadata_json, '$.experimentId')), '') IN ('', 'null')
            AND :hasCodes = 1 AND (utm_campaign IN (:codes) OR utm_content IN (:codes))))
        AND occurred_at >= :periodStart AND occurred_at <= :periodEnd
      """;
  private static final String HUMAN = " AND traffic_quality = 'HUMAN' ";
  private static final String SESSION = "COALESCE(NULLIF(session_id, ''), event_id)";
  private static final String SCROLL =
      "COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(metadata_json, '$.maxScrollDepthPercent')) AS SIGNED),"
          + "CAST(JSON_UNQUOTE(JSON_EXTRACT(metadata_json, '$.scrollDepthPercent')) AS SIGNED),0)";
  private final NamedParameterJdbcTemplate jdbc;

  /** Usa exclusivamente a base canônica do backend principal. */
  public ExperimentPdeAnalyticsRepository(JdbcTemplate jdbc) {
    this.jdbc = new NamedParameterJdbcTemplate(jdbc);
  }

  /** Busca identificadores e UTMs efetivamente vinculados às campanhas do experimento. */
  public List<String> attributionCodes(Long experimentId) {
    return jdbc.queryForList(
        """
        SELECT DISTINCT code FROM (
          SELECT c.id AS code FROM facebook_ads_campaign c WHERE c.experiment_id = :id
          UNION ALL SELECT c.external_id FROM facebook_ads_campaign c WHERE c.experiment_id = :id
          UNION ALL SELECT s.id FROM facebook_ads_campaign c JOIN facebook_ads_ad_set s ON s.campaign_id=c.id WHERE c.experiment_id=:id
          UNION ALL SELECT s.external_id FROM facebook_ads_campaign c JOIN facebook_ads_ad_set s ON s.campaign_id=c.id WHERE c.experiment_id=:id
          UNION ALL SELECT a.id FROM facebook_ads_campaign c JOIN facebook_ads_ad_set s ON s.campaign_id=c.id JOIN facebook_ads_ad a ON a.adset_id=s.id WHERE c.experiment_id=:id
          UNION ALL SELECT a.external_id FROM facebook_ads_campaign c JOIN facebook_ads_ad_set s ON s.campaign_id=c.id JOIN facebook_ads_ad a ON a.adset_id=s.id WHERE c.experiment_id=:id
          UNION ALL SELECT u.utm_campaign FROM facebook_ads_campaign c JOIN facebook_ads_ad_set s ON s.campaign_id=c.id JOIN facebook_ads_ad a ON a.adset_id=s.id JOIN facebook_ads_ad_tracking_utm u ON u.ad_id=a.id WHERE c.experiment_id=:id
          UNION ALL SELECT u.utm_content FROM facebook_ads_campaign c JOIN facebook_ads_ad_set s ON s.campaign_id=c.id JOIN facebook_ads_ad a ON a.adset_id=s.id JOIN facebook_ads_ad_tracking_utm u ON u.ad_id=a.id WHERE c.experiment_id=:id
        ) codes WHERE code IS NOT NULL AND TRIM(code) <> ''
        """,
        Map.of("id", experimentId),
        String.class);
  }

  /** Agrega eventos completos; as listas de detalhe nunca limitam os totais comerciais. */
  public PdeAnalyticsSummary summarize(
      Long experimentId, String product, String version, List<String> codes) {
    return summarize(experimentId, product, version, codes, null, null);
  }

  /** Agrega somente a janela autorizada quando ela for fornecida pelo ciclo comercial. */
  public PdeAnalyticsSummary summarize(
      Long experimentId,
      String product,
      String version,
      List<String> codes,
      java.time.Instant periodStart,
      java.time.Instant periodEnd) {
    Map<String, Object> parameters =
        parameters(experimentId, product, version, codes, periodStart, periodEnd);
    List<PdeEventMetric> events =
        jdbc.query(
            "SELECT event_type, COUNT(*) AS total "
                + SCOPE
                + HUMAN
                + " GROUP BY event_type ORDER BY event_type",
            parameters,
            (rs, row) -> new PdeEventMetric(rs.getString("event_type"), rs.getLong("total")));
    Map<String, Long> counts = new LinkedHashMap<>();
    events.forEach(event -> counts.put(event.eventType(), event.total()));
    Totals totals =
        jdbc.queryForObject(
            "SELECT COUNT(*) AS total, COUNT(DISTINCT "
                + SESSION
                + ") AS sessions, COUNT(DISTINCT NULLIF(visitor_id, '')) AS visitors, COALESCE(SUM(visible_ms),0) AS visible, MAX(occurred_at) AS latest "
                + SCOPE
                + HUMAN,
            parameters,
            (rs, row) ->
                new Totals(
                    rs.getLong("total"),
                    rs.getLong("sessions"),
                    rs.getLong("visitors"),
                    rs.getLong("visible"),
                    instant(rs, "latest")));
    List<PdeTrafficQualityMetric> quality =
        jdbc.query(
            "SELECT COALESCE(traffic_quality,'UNKNOWN') AS quality, COUNT(*) AS total, COUNT(DISTINCT "
                + SESSION
                + ") AS sessions "
                + SCOPE
                + " GROUP BY COALESCE(traffic_quality,'UNKNOWN') ORDER BY quality",
            parameters,
            (rs, row) ->
                new PdeTrafficQualityMetric(
                    rs.getString("quality"),
                    rs.getString("quality"),
                    rs.getLong("sessions"),
                    rs.getLong("total"),
                    0));
    long rawEvents = quality.stream().mapToLong(PdeTrafficQualityMetric::events).sum();
    long rawSessions =
        jdbc.queryForObject(
            "SELECT COUNT(DISTINCT " + SESSION + ") " + SCOPE, parameters, Long.class);
    List<PdeDeviceMetric> devices =
        jdbc.query(
            "SELECT COALESCE(device_type,'unknown') AS device, COUNT(DISTINCT "
                + SESSION
                + ") AS sessions "
                + SCOPE
                + HUMAN
                + " GROUP BY COALESCE(device_type,'unknown') ORDER BY device",
            parameters,
            (rs, row) ->
                new PdeDeviceMetric(
                    rs.getString("device"),
                    rs.getString("device"),
                    rs.getLong("sessions"),
                    percentage(rs.getLong("sessions"), totals.sessions())));
    List<PdeScreenSizeMetric> screens =
        jdbc.query(
            "SELECT screen_width, screen_height, COUNT(DISTINCT "
                + SESSION
                + ") AS sessions "
                + SCOPE
                + HUMAN
                + " GROUP BY screen_width,screen_height ORDER BY screen_width,screen_height",
            parameters,
            (rs, row) ->
                new PdeScreenSizeMetric(
                    rs.getInt("screen_width") + "x" + rs.getInt("screen_height"),
                    rs.getInt("screen_width") + " × " + rs.getInt("screen_height"),
                    (Integer) rs.getObject("screen_width"),
                    (Integer) rs.getObject("screen_height"),
                    rs.getLong("sessions"),
                    percentage(rs.getLong("sessions"), totals.sessions())));
    List<PdeTrafficSourceMetric> sources = trafficSources(parameters);
    long entries = count(counts, "PED_ENTRY");
    long presence = count(counts, "PRESENCE_MAP_CHOICE_SELECTED");
    long diagnostic = count(counts, "DIAGNOSTIC_CHOICE_SELECTED");
    long partial = count(counts, "VIDEO_PROGRESS_25", "VIDEO_PROGRESS_50", "VIDEO_PROGRESS_75");
    long completed = count(counts, "VIDEO_COMPLETED");
    long login = count(counts, "LOGIN_STARTED");
    long paywall = count(counts, "PAYWALL_VIEWED");
    long checkout = count(counts, "CHECKOUT_STARTED");
    long approved = count(counts, "SUBSCRIPTION_APPROVED");
    long subscription = count(counts, "SUBSCRIPTION_CLICKED");
    return new PdeAnalyticsSummary(
        product,
        version,
        totals.events(),
        rawEvents,
        totals.visitors(),
        totals.sessions(),
        rawSessions,
        totals.sessions(),
        qualitySessions(quality, "BOT_SUSPECTED"),
        qualitySessions(quality, "PLATFORM_CRAWLER"),
        qualitySessions(quality, "INTERNAL_QA"),
        qualitySessions(quality, "UNKNOWN"),
        entries,
        count(counts, "PAGE_VIEW"),
        login,
        count(counts, "LOGIN_COMPLETED"),
        paywall,
        subscription,
        approved,
        count(counts, "ACCESS_RELEASED"),
        count(counts, "FIRST_USE"),
        checkout,
        totals.visible(),
        totals.latest(),
        events,
        List.of(
            new PdeExperienceVersionMetric(
                version,
                totals.events(),
                totals.sessions(),
                entries,
                presence,
                diagnostic,
                partial,
                completed,
                login,
                paywall,
                subscription,
                checkout,
                approved)),
        sources,
        quality.stream()
            .map(
                q ->
                    new PdeTrafficQualityMetric(
                        q.trafficQuality(),
                        q.label(),
                        q.sessions(),
                        q.events(),
                        percentage(q.sessions(), rawSessions)))
            .toList(),
        devices,
        screens,
        journeys(experimentId, parameters));
  }

  /**
   * Concilia eventos comerciais internos do mesmo recorte atribuído sem devolver IDs de pagamento.
   */
  public PdeCommercialOutcomeSummary commercialOutcomes(
      Long experimentId, String product, String version, List<String> codes) {
    return commercialOutcomes(experimentId, product, version, codes, null, null);
  }

  /** Concilia desfechos comerciais limitados à janela autorizada do ciclo. */
  public PdeCommercialOutcomeSummary commercialOutcomes(
      Long experimentId,
      String product,
      String version,
      List<String> codes,
      java.time.Instant periodStart,
      java.time.Instant periodEnd) {
    Map<String, Object> parameters =
        parameters(experimentId, product, version, codes, periodStart, periodEnd);
    List<CommercialEvent> rows =
        jdbc.query(
            "SELECT event_type,"
                + "COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.paymentId')),'null'),"
                + "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.pepperTransactionId')),'null')) AS payment_reference,"
                + "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.accessReferenceHash')),'null') AS access_reference,"
                + "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.amountBrl')),'null') AS amount_brl,"
                + "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.pepperAmountCents')),'null') AS amount_cents,"
                + "COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.currency')),'null'),"
                + "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.pepperCurrency')),'null')) AS currency,"
                + "JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.useful')) AS useful,"
                + "JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.easy')) AS easy,"
                + "JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.applicable')) AS applicable "
                + SCOPE
                + HUMAN
                + " AND event_type IN ('PURCHASE_COMPLETED','REFUND_CONFIRMED','ACCESS_RELEASED','DELIVERY_COMPLETED','FIRST_USE','MISSION_FEEDBACK_SUBMITTED')"
                + " ORDER BY occurred_at,id",
            parameters,
            (rs, row) ->
                new CommercialEvent(
                    rs.getString("event_type"),
                    rs.getString("payment_reference"),
                    rs.getString("access_reference"),
                    rs.getString("amount_brl"),
                    rs.getString("amount_cents"),
                    rs.getString("currency"),
                    rs.getString("useful"),
                    rs.getString("easy"),
                    rs.getString("applicable")));
    Map<String, CommercialEvent> purchases = new LinkedHashMap<>();
    Map<String, CommercialEvent> refunds = new LinkedHashMap<>();
    Map<String, String> accessByPayment = new LinkedHashMap<>();
    Set<String> deliveries = new LinkedHashSet<>();
    Set<String> firstUses = new LinkedHashSet<>();
    Set<String> feedback = new LinkedHashSet<>();
    Set<String> positiveFeedback = new LinkedHashSet<>();
    boolean referencesComplete = true;
    boolean amountsComplete = true;
    boolean valueReferencesComplete = true;
    int anonymousReference = 0;
    for (CommercialEvent row : rows) {
      if (Set.of("ACCESS_RELEASED", "DELIVERY_COMPLETED", "FIRST_USE", "MISSION_FEEDBACK_SUBMITTED")
          .contains(row.eventType())) {
        if (row.accessReference() == null || row.accessReference().isBlank()) {
          valueReferencesComplete = false;
          continue;
        }
        switch (row.eventType()) {
          case "ACCESS_RELEASED" -> {
            if (row.paymentReference() == null || row.paymentReference().isBlank()) {
              valueReferencesComplete = false;
            } else {
              String previous =
                  accessByPayment.putIfAbsent(row.paymentReference(), row.accessReference());
              if (previous != null && !previous.equals(row.accessReference()))
                valueReferencesComplete = false;
            }
          }
          case "DELIVERY_COMPLETED" -> deliveries.add(row.accessReference());
          case "FIRST_USE" -> firstUses.add(row.accessReference());
          case "MISSION_FEEDBACK_SUBMITTED" -> {
            feedback.add(row.accessReference());
            if ("Sim".equalsIgnoreCase(row.useful())
                && "Sim".equalsIgnoreCase(row.easy())
                && "Sim".equalsIgnoreCase(row.applicable()))
              positiveFeedback.add(row.accessReference());
          }
          default -> throw new IllegalStateException("Evento de valor sem tratamento canônico.");
        }
        continue;
      }
      String reference = row.paymentReference();
      if (reference == null || reference.isBlank()) {
        referencesComplete = false;
        reference = "missing-" + anonymousReference++;
      }
      BigDecimal amount = row.amountBrl();
      if (amount == null || amount.signum() < 0 || !"BRL".equalsIgnoreCase(row.currency()))
        amountsComplete = false;
      ("PURCHASE_COMPLETED".equals(row.eventType()) ? purchases : refunds)
          .putIfAbsent(reference, row);
    }
    BigDecimal gross = total(purchases.values());
    BigDecimal refunded = total(refunds.values());
    boolean refundsMatchPurchases =
        refunds.entrySet().stream()
            .allMatch(
                entry -> {
                  CommercialEvent purchase = purchases.get(entry.getKey());
                  BigDecimal refundAmount = entry.getValue().amountBrl();
                  BigDecimal purchaseAmount = purchase == null ? null : purchase.amountBrl();
                  return purchaseAmount != null
                      && refundAmount != null
                      && refundAmount.compareTo(purchaseAmount) <= 0;
                });
    if (!purchases.keySet().containsAll(accessByPayment.keySet())) valueReferencesComplete = false;
    if (new LinkedHashSet<>(accessByPayment.values()).size() != accessByPayment.size())
      valueReferencesComplete = false;
    Set<String> activePayments = new LinkedHashSet<>(purchases.keySet());
    activePayments.removeAll(refunds.keySet());
    long releasedNetSales =
        correlated(activePayments, accessByPayment, Set.copyOf(accessByPayment.values()));
    long deliveredNetSales = correlated(activePayments, accessByPayment, deliveries);
    long firstUseNetSales = correlated(activePayments, accessByPayment, firstUses);
    long satisfactionResponses = correlated(activePayments, accessByPayment, feedback);
    long positiveSatisfactionResponses =
        correlated(activePayments, accessByPayment, positiveFeedback);
    return new PdeCommercialOutcomeSummary(
        purchases.size(),
        refunds.size(),
        gross,
        refunded,
        gross.subtract(refunded).setScale(2, RoundingMode.HALF_UP),
        referencesComplete,
        amountsComplete,
        refundsMatchPurchases,
        valueReferencesComplete,
        releasedNetSales,
        deliveredNetSales,
        firstUseNetSales,
        satisfactionResponses,
        positiveSatisfactionResponses);
  }

  /**
   * Consolida vinte jornadas recentes do mesmo recorte antes de pseudonimizar seus identificadores.
   */
  private List<PdeSessionJourney> journeys(Long experimentId, Map<String, Object> parameters) {
    Map<String, List<String>> screens = new LinkedHashMap<>();
    Map<String, List<String>> sections = new LinkedHashMap<>();
    String sql =
        "SELECT j.*,last_event.event_type AS last_type,last_event.action_name AS last_action FROM (SELECT "
            + SESSION
            + " AS session_key,MIN(visitor_id) AS visitor,"
            + "MIN(occurred_at) AS first_at,MAX(occurred_at) AS last_at,"
            // Apenas o primeiro ID é usado; truncamento do restante da lista não altera a última
            // ação.
            + "SUBSTRING_INDEX(GROUP_CONCAT(id ORDER BY occurred_at DESC,id DESC),',',1) AS last_id,SUM(visible_ms) AS visible,"
            + "MAX("
            + SCROLL
            + ") AS scroll,"
            + "SUM(CASE WHEN event_type IN ('UI_CLICK','LINK_CLICK') AND (LOWER(COALESCE(action_name,'')) LIKE '%cta%' OR LOWER(COALESCE(JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.elementText')),'')) REGEXP 'acesso|diagnóstico|diagnostico|liberar|começar|comecar') THEN 1 ELSE 0 END) AS cta,"
            + totalSql("FIELD_FOCUS", "field_focus")
            + ","
            + totalSql("FIELD_INPUT", "field_input")
            + ","
            + totalSql("FIELD_FILLED", "field_filled")
            + ","
            + totalSql("LOGIN_STARTED", "login_started")
            + ","
            + totalSql("LOGIN_COMPLETED", "login_completed")
            + ","
            + totalSql("PAYWALL_VIEWED", "paywall")
            + ","
            + totalSql("CHECKOUT_STARTED", "checkout")
            + ","
            + totalSql("SUBSCRIPTION_APPROVED", "approved")
            + " "
            + SCOPE
            + HUMAN
            + " GROUP BY "
            + SESSION
            + " ORDER BY MAX(occurred_at) DESC,MAX(id) DESC LIMIT 20) j "
            + "JOIN pde_funnel_event last_event ON last_event.id=j.last_id ORDER BY j.last_at DESC,j.last_id DESC";
    List<PdeSessionJourney> journeys =
        jdbc.query(
            sql,
            parameters,
            (rs, row) ->
                new PdeSessionJourney(
                    anonymous(experimentId, "session", rs.getString("session_key")),
                    anonymous(experimentId, "visitor", rs.getString("visitor")),
                    null,
                    null,
                    "HUMAN",
                    "EXPERIMENT_ATTRIBUTED",
                    null,
                    instant(rs, "first_at"),
                    instant(rs, "last_at"),
                    rs.getLong("visible"),
                    rs.getLong("scroll"),
                    screens.computeIfAbsent(rs.getString("session_key"), key -> new ArrayList<>()),
                    sections.computeIfAbsent(rs.getString("session_key"), key -> new ArrayList<>()),
                    rs.getLong("field_focus") > 0,
                    rs.getLong("field_input") > 0,
                    rs.getLong("field_filled") > 0,
                    rs.getLong("cta") > 0,
                    rs.getLong("login_started") > 0,
                    rs.getLong("login_completed") > 0,
                    rs.getLong("paywall") > 0,
                    rs.getLong("checkout") > 0,
                    rs.getLong("approved") > 0,
                    "NOT_INFERRED",
                    rs.getString("last_type"),
                    rs.getString("last_action")));
    if (!journeys.isEmpty()) {
      Map<String, Object> scoped = new LinkedHashMap<>(parameters);
      scoped.put("sessions", screens.keySet());
      jdbc.query(
          "SELECT DISTINCT "
              + SESSION
              + " AS session_key,section_id,"
              + "JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.screenName')) AS screen "
              + SCOPE
              + HUMAN
              + " AND "
              + SESSION
              + " IN (:sessions) ORDER BY session_key,screen,section_id",
          scoped,
          (org.springframework.jdbc.core.RowCallbackHandler)
              rs -> {
                addName(screens.get(rs.getString("session_key")), rs.getString("screen"));
                addName(sections.get(rs.getString("session_key")), rs.getString("section_id"));
              });
    }
    return journeys;
  }

  /** Preserva nomes técnicos distintos das telas e seções do recorte consultado. */
  private void addName(List<String> target, String name) {
    if (name != null && !name.isBlank() && !target.contains(name)) target.add(name);
  }

  /** Lê uma janela de eventos atribuídos sem conteúdo digitado, e-mail, URL, IP ou tokens. */
  public List<
          com.marketinghub.experiment.funnel.service.analytics
              .ExperimentLandingAnalyticsDetailedEventDto>
      detailedEvents(
          Long experimentId, String product, String version, List<String> codes, int limit) {
    Map<String, Object> parameters =
        new LinkedHashMap<>(parameters(experimentId, product, version, codes));
    parameters.put("limit", Math.max(1, Math.min(limit, 2000)));
    return jdbc.query(
        "SELECT id,"
            + SESSION
            + " AS session_key,visitor_id,event_type,section_id,device_type,visible_ms,occurred_at,action_name,"
            + SCROLL
            + " AS scroll,JSON_UNQUOTE(JSON_EXTRACT(metadata_json,'$.screenName')) AS screen "
            + SCOPE
            + HUMAN
            + " ORDER BY occurred_at DESC,id DESC LIMIT :limit",
        parameters,
        (rs, row) ->
            new com.marketinghub.experiment.funnel.service.analytics
                .ExperimentLandingAnalyticsDetailedEventDto(
                rs.getLong("id"),
                anonymous(experimentId, "visitor", rs.getString("visitor_id")),
                anonymous(experimentId, "session", rs.getString("session_key")),
                rs.getString("event_type"),
                rs.getString("section_id"),
                java.time.Instant.parse(instant(rs, "occurred_at")),
                "HUMAN",
                "EXPERIMENT_ATTRIBUTED",
                attributes(rs)));
  }

  /** Entrega somente atributos técnicos; o JSON bruto e conteúdos de campos nunca saem da base. */
  private Map<String, String> attributes(ResultSet rs) throws SQLException {
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("visibleMs", Long.toString(rs.getLong("visible_ms")));
    attributes.put("maxScrollDepthPercent", Long.toString(rs.getLong("scroll")));
    for (var field :
        Map.of("deviceType", "device_type", "actionName", "action_name", "screenName", "screen")
            .entrySet()) {
      String value = rs.getString(field.getValue());
      if (value != null && !value.isBlank()) attributes.put(field.getKey(), value);
    }
    return attributes;
  }

  /** Parametriza atribuição UTM ou referência explícita do experimento usada pelo canal direto. */
  private Map<String, Object> parameters(
      Long id, String product, String version, List<String> codes) {
    return parameters(id, product, version, codes, null, null);
  }

  /**
   * Converte a janela UTC para o DATETIME operacional e usa limites MySQL quando ela não existe.
   */
  private Map<String, Object> parameters(
      Long id,
      String product,
      String version,
      List<String> codes,
      java.time.Instant periodStart,
      java.time.Instant periodEnd) {
    ZoneId zone = ZoneId.of("America/Sao_Paulo");
    var parameters = new LinkedHashMap<String, Object>();
    parameters.put("product", product);
    parameters.put("version", version);
    parameters.put("experimentId", String.valueOf(id));
    parameters.put("hasCodes", codes.isEmpty() ? 0 : 1);
    parameters.put("codes", codes.isEmpty() ? List.of("") : codes);
    parameters.put(
        "periodStart",
        periodStart == null
            ? java.time.LocalDateTime.of(1000, 1, 1, 0, 0)
            : java.time.LocalDateTime.ofInstant(periodStart, zone));
    parameters.put(
        "periodEnd",
        periodEnd == null
            ? java.time.LocalDateTime.of(9999, 12, 31, 23, 59, 59)
            : java.time.LocalDateTime.ofInstant(periodEnd, zone));
    return parameters;
  }

  /**
   * Pseudonimiza identificadores dentro do experimento, sem permitir correlação global de pessoas.
   */
  private String anonymous(Long id, String kind, String value) {
    if (value == null || value.isBlank()) return null;
    try {
      byte[] digest =
          java.security.MessageDigest.getInstance("SHA-256")
              .digest(
                  (id + ":" + kind + ":" + value)
                      .getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return kind + "-" + java.util.HexFormat.of().formatHex(digest).substring(0, 24);
    } catch (java.security.NoSuchAlgorithmException ex) {
      org.slf4j.LoggerFactory.getLogger(getClass())
          .error("Falha ao pseudonimizar métricas PDE; experimentId={}", id, ex);
      throw new IllegalStateException("SHA-256 indisponível", ex);
    }
  }

  /** Mantém contagens por origem sem o corte global das vinte campanhas mais volumosas. */
  private List<PdeTrafficSourceMetric> trafficSources(Map<String, Object> parameters) {
    String sql =
        "SELECT utm_source,utm_medium,utm_campaign,utm_content,COUNT(DISTINCT "
            + SESSION
            + ") AS sessions,"
            + totalSql("PED_ENTRY", "entries")
            + ","
            + "SUM(CASE WHEN event_type IN ('DIAGNOSTIC_CHOICE_SELECTED','PRESENCE_MAP_CHOICE_SELECTED') THEN 1 ELSE 0 END) AS interaction"
            + ","
            + totalSql("VIDEO_PROGRESS_25", "partial25")
            + ","
            + totalSql("VIDEO_PROGRESS_50", "partial50")
            + ","
            + totalSql("VIDEO_PROGRESS_75", "partial75")
            + ","
            + totalSql("VIDEO_COMPLETED", "completed")
            + ","
            + totalSql("LOGIN_STARTED", "login")
            + ","
            + totalSql("PAYWALL_VIEWED", "paywall")
            + ","
            + totalSql("CHECKOUT_STARTED", "checkout")
            + ","
            + totalSql("SUBSCRIPTION_APPROVED", "approved")
            + ",COALESCE(SUM(visible_ms),0) AS visible,MAX(occurred_at) AS latest "
            + SCOPE
            + HUMAN
            + " GROUP BY utm_source,utm_medium,utm_campaign,utm_content ORDER BY utm_campaign,utm_content,utm_source,utm_medium";
    return jdbc.query(
        sql,
        parameters,
        (rs, row) -> {
          long sessions = rs.getLong("sessions");
          return new PdeTrafficSourceMetric(
              "Atribuído",
              rs.getString("utm_source"),
              rs.getString("utm_medium"),
              rs.getString("utm_campaign"),
              rs.getString("utm_content"),
              sessions,
              rs.getLong("entries"),
              rs.getLong("interaction"),
              rs.getLong("partial25") + rs.getLong("partial50") + rs.getLong("partial75"),
              rs.getLong("completed"),
              rs.getLong("login"),
              rs.getLong("paywall"),
              rs.getLong("checkout"),
              rs.getLong("approved"),
              percentage(rs.getLong("interaction"), sessions),
              percentage(rs.getLong("paywall"), sessions),
              percentage(rs.getLong("checkout"), sessions),
              percentage(rs.getLong("approved"), sessions),
              rs.getLong("visible"),
              instant(rs, "latest"));
        });
  }

  /** Gera expressão somente com os nomes internos fixos de evento e coluna. */
  private String totalSql(String event, String alias) {
    return "SUM(CASE WHEN event_type='" + event + "' THEN 1 ELSE 0 END) AS " + alias;
  }

  /** Conta sessões da classificação pedida mantendo tráfego técnico separado. */
  private long qualitySessions(List<PdeTrafficQualityMetric> quality, String key) {
    return quality.stream()
        .filter(q -> key.equals(q.trafficQuality()))
        .mapToLong(PdeTrafficQualityMetric::sessions)
        .sum();
  }

  /** Soma apenas eventos explicitamente associados à métrica. */
  private long count(Map<String, Long> events, String... types) {
    return java.util.Arrays.stream(types).mapToLong(type -> events.getOrDefault(type, 0L)).sum();
  }

  /** Converte DATETIME operacional sem depender do fuso da JVM ou do navegador. */
  private String instant(ResultSet rs, String column) throws SQLException {
    java.sql.Timestamp value = rs.getTimestamp(column);
    return value == null
        ? null
        : value.toLocalDateTime().atZone(ZoneId.of("America/Sao_Paulo")).toInstant().toString();
  }

  /** Calcula percentual apenas para um denominador existente. */
  private double percentage(long value, long total) {
    return total == 0 ? 0 : value * 100.0 / total;
  }

  /**
   * Soma valores financeiros já normalizados, preservando zero quando uma fonte está incompleta.
   */
  private BigDecimal total(java.util.Collection<CommercialEvent> events) {
    return events.stream()
        .map(CommercialEvent::amountBrl)
        .filter(java.util.Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .setScale(2, RoundingMode.HALF_UP);
  }

  /** Conta compras líquidas cuja referência de acesso alcançou o marco informado. */
  private long correlated(
      Set<String> activePayments,
      Map<String, String> accessByPayment,
      Set<String> reachedAccesses) {
    return activePayments.stream()
        .map(accessByPayment::get)
        .filter(java.util.Objects::nonNull)
        .filter(reachedAccesses::contains)
        .count();
  }

  /** Mantém uma linha comercial apenas durante a conciliação, sem expor sua referência. */
  private record CommercialEvent(
      String eventType,
      String paymentReference,
      String accessReference,
      String amountBrlText,
      String amountCentsText,
      String currency,
      String useful,
      String easy,
      String applicable) {
    /** Converte reais ou centavos canônicos em BRL sem aceitar conteúdo financeiro ilegível. */
    private BigDecimal amountBrl() {
      try {
        if (amountBrlText != null && !amountBrlText.isBlank())
          return new BigDecimal(amountBrlText).setScale(2, RoundingMode.HALF_UP);
        if (amountCentsText != null && !amountCentsText.isBlank())
          return new BigDecimal(amountCentsText).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        return null;
      } catch (NumberFormatException ex) {
        return null;
      }
    }
  }

  /** Conserva os totais SQL antes de montar o contrato compartilhado. */
  private record Totals(long events, long sessions, long visitors, long visible, String latest) {}
}
