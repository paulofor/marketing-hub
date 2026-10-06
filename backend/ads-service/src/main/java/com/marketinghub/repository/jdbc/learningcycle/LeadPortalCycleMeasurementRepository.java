package com.marketinghub.repository.jdbc.learningcycle;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.reconcileMeasurement.LeadPortalCycleMeasurement;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.reconcileMeasurement.LeadPortalCycleMeasurement.Payments;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Responsabilidade: ler as fontes comerciais do Lead Portal no recorte exato do ciclo. */
@Repository
public class LeadPortalCycleMeasurementRepository {
  private final JdbcTemplate jdbc;

  /** Recebe a única conexão canônica do backend, sem consultar módulos externos. */
  public LeadPortalCycleMeasurementRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Concilia publicação, tráfego segregado e pagamentos persistidos dentro da janela. */
  public LeadPortalCycleMeasurement read(Long experimentId, Instant start, Instant end) {
    var from = Timestamp.from(start);
    var until = Timestamp.from(end);
    var publications =
        jdbc.query(
            """
            SELECT id, sales_page_url FROM gera_sales_page_publication_audit
            WHERE experiment_id = ? AND published_at <= ?
            ORDER BY published_at DESC, id DESC LIMIT 1
            """,
            (rs, row) -> new Publication(rs.getLong("id"), rs.getString("sales_page_url")),
            experimentId,
            until);
    var publication = publications.isEmpty() ? null : publications.getFirst();
    var funnel =
        jdbc.queryForObject(
            """
            SELECT COUNT(*) AS raw_events, COUNT(DISTINCT a.session_id) AS raw_sessions,
              SUM(CASE WHEN a.commercial_human THEN 1 ELSE 0 END) AS human_events,
              COUNT(DISTINCT CASE WHEN a.commercial_human
                AND LOWER(a.event_type) = 'page_view' THEN NULLIF(a.visitor_id, '') END) AS visitors,
              COUNT(DISTINCT CASE WHEN a.commercial_human
                AND LOWER(a.event_type) = 'page_view' THEN NULLIF(a.session_id, '') END) AS sessions,
              SUM(CASE WHEN a.commercial_human
                AND LOWER(a.event_type) = 'page_view' THEN 1 ELSE 0 END) AS page_views,
              SUM(CASE WHEN a.commercial_human
                AND LOWER(a.event_type) = 'checkout_click' THEN 1 ELSE 0 END) AS checkouts,
              MAX(CASE WHEN a.commercial_human THEN a.occurred_at END) AS last_event
            FROM (
              SELECT a.*, (a.traffic_quality = 'HUMAN'
                AND LOWER(COALESCE(f.payload, '')) NOT LIKE '%mh_test=1%'
                AND LOWER(COALESCE(f.payload, '')) NOT LIKE '%mh_audit=%'
                AND COALESCE(f.campaign_code, '') <> '__mh_internal_test__') AS commercial_human
              FROM experiment_landing_analytics_event a
              JOIN experiment_funnel_event f ON f.id = a.funnel_event_id
                AND f.experiment_id = a.experiment_id
              WHERE a.experiment_id = ? AND a.occurred_at >= ? AND a.occurred_at <= ?
            ) a
            """,
            (rs, row) ->
                new Funnel(
                    rs.getLong("human_events"),
                    rs.getLong("raw_events"),
                    rs.getLong("visitors"),
                    rs.getLong("sessions"),
                    rs.getLong("raw_sessions"),
                    rs.getLong("page_views"),
                    rs.getLong("checkouts"),
                    instant(rs.getTimestamp("last_event"))),
            experimentId,
            from,
            until);
    Long flows =
        jdbc.queryForObject(
            """
            SELECT COUNT(*) FROM lead_portal_flow f JOIN experiment e ON e.id = ?
            WHERE f.id = e.lead_portal_flow_id OR f.experiment_id = e.id
            """,
            Long.class,
            experimentId);
    return new LeadPortalCycleMeasurement(
        publication == null ? null : publication.id(),
        publication == null ? null : publication.url(),
        flows != null && flows > 0,
        funnel.humanEvents(),
        funnel.rawEvents(),
        funnel.visitors(),
        funnel.sessions(),
        funnel.rawSessions(),
        funnel.pageViews(),
        funnel.checkouts(),
        funnel.lastEvent(),
        payments(experimentId, from, until));
  }

  /** Lê checkout e desfechos financeiros; fluxo compartilhado exige atribuição própria. */
  private Payments payments(Long experimentId, Timestamp start, Timestamp end) {
    List<Payment> rows =
        jdbc.query(
            """
            SELECT p.mp_payment_id, p.amount, p.currency, p.mp_status,
              (p.payment_approved_at IS NULL
                AND LOWER(p.mp_status) IN ('approved', 'refunded', 'charged_back')) AS undated_payment,
              (p.delivered_at <= ? AND p.zip_generated_at <= ?
                AND NULLIF(TRIM(p.zip_object_key), '') IS NOT NULL) AS delivered,
              (p.checkout_accessed_at >= ? AND p.checkout_accessed_at <= ?) AS checkout_access,
              ((p.payment_approved_at IS NOT NULL
                OR LOWER(p.mp_status) IN ('approved', 'refunded', 'charged_back'))
                AND COALESCE(p.payment_approved_at, p.updated_at) >= ?
                AND COALESCE(p.payment_approved_at, p.updated_at) <= ?) AS financial_outcome,
              CASE WHEN s.campaign_code = CONCAT(e.id, '')
                OR EXISTS (SELECT 1 FROM facebook_ads_campaign c WHERE c.experiment_id = e.id
                  AND (s.campaign_code = c.id OR s.campaign_code = c.external_id))
                OR ((s.campaign_code IS NULL OR s.campaign_code = '') AND f.experiment_id = e.id
                  AND NOT EXISTS (SELECT 1 FROM experiment other WHERE other.id <> e.id
                    AND other.lead_portal_flow_id = f.id)) THEN 'EXACT'
                WHEN EXISTS (SELECT 1 FROM experiment other WHERE other.id <> e.id
                  AND s.campaign_code = CONCAT(other.id, ''))
                OR EXISTS (SELECT 1 FROM facebook_ads_campaign c WHERE c.experiment_id <> e.id
                  AND (s.campaign_code = c.id OR s.campaign_code = c.external_id)) THEN 'OTHER'
                ELSE 'AMBIGUOUS' END AS attribution
            FROM lead_portal_purchase p JOIN flow_submissions s ON s.id = p.submission_id
            JOIN lead_portal_flow f ON f.slug = s.flow_slug JOIN experiment e ON e.id = ?
            WHERE (f.id = e.lead_portal_flow_id OR f.experiment_id = e.id)
              AND (s.campaign_code IS NULL OR s.campaign_code <> '__mh_internal_test__')
              AND LOWER(COALESCE(s.answers, '')) NOT LIKE '%mh_test=1%'
              AND LOWER(COALESCE(s.answers, '')) NOT LIKE '%mh_audit=%'
              AND LOWER(REPLACE(REPLACE(REPLACE(REPLACE(COALESCE(p.mp_payment_payload, ''),
                ' ', ''), CHAR(10), ''), CHAR(13), ''), CHAR(9), ''))
                NOT LIKE '%"live_mode":false%'
              AND (((p.payment_approved_at IS NOT NULL
                OR LOWER(p.mp_status) IN ('approved', 'refunded', 'charged_back'))
              AND COALESCE(p.payment_approved_at, p.updated_at) >= ?
              AND COALESCE(p.payment_approved_at, p.updated_at) <= ?)
                OR (p.checkout_accessed_at >= ? AND p.checkout_accessed_at <= ?)
                OR LOWER(p.mp_status) IN ('refunded', 'charged_back')
                OR (p.payment_approved_at IS NULL
                  AND LOWER(p.mp_status) IN ('approved', 'refunded', 'charged_back')))
            """,
            (rs, row) ->
                new Payment(
                    rs.getString("mp_payment_id"),
                    rs.getBigDecimal("amount"),
                    rs.getString("currency"),
                    rs.getString("mp_status"),
                    rs.getBoolean("delivered"),
                    rs.getBoolean("checkout_access"),
                    rs.getBoolean("financial_outcome"),
                    rs.getBoolean("undated_payment"),
                    rs.getString("attribution")),
            end,
            end,
            start,
            end,
            start,
            end,
            experimentId,
            start,
            end,
            start,
            end);
    var references = new HashSet<String>();
    long checkouts = 0, purchases = 0, deliveries = 0;
    BigDecimal gross = BigDecimal.ZERO.setScale(2);
    for (var payment : rows) {
      if ("OTHER".equals(payment.attribution())) continue;
      if (!"EXACT".equals(payment.attribution()))
        return blockedPayments("Compra no checkout compartilhado sem atribuição ao experimento.");
      if (payment.undatedPayment())
        return blockedPayments(
            "Pagamento aprovado sem data de aprovação não permite atribuir receita à janela.");
      if ("refunded".equalsIgnoreCase(payment.status())
          || "charged_back".equalsIgnoreCase(payment.status()))
        return blockedPayments(
            "Reembolso ou status financeiro divergente exige conciliação datada do provedor.");
      if (payment.checkoutAccess()) checkouts++;
      if (!payment.financialOutcome()) continue;
      if (payment.reference() == null
          || payment.reference().isBlank()
          || !references.add(payment.reference()))
        return blockedPayments(
            "Pagamento sem referência única; concilie duplicidades antes de decidir.");
      if (payment.amount() == null
          || payment.amount().signum() <= 0
          || !"BRL".equalsIgnoreCase(payment.currency()))
        return blockedPayments("Pagamento sem valor positivo e moeda BRL verificáveis.");
      if (!"approved".equalsIgnoreCase(payment.status()))
        return blockedPayments(
            "Reembolso ou status financeiro divergente exige conciliação datada do provedor.");
      purchases++;
      gross = gross.add(payment.amount());
      if (payment.delivered()) deliveries++;
    }
    return new Payments(
        checkouts, purchases, 0, gross, BigDecimal.ZERO.setScale(2), deliveries, null);
  }

  /** Conserva a ausência de conciliação como bloqueio, sem publicar totais presumidos. */
  private Payments blockedPayments(String blocker) {
    return new Payments(0, 0, 0, null, null, 0, blocker);
  }

  /** Converte a data SQL sem substituir ausência por horário artificial. */
  private Instant instant(Timestamp value) {
    return value == null ? null : value.toInstant();
  }

  /** Responsabilidade: transportar somente a identidade da publicação histórica consultada. */
  private record Publication(Long id, String url) {}

  /** Responsabilidade: transportar contagens agregadas de tráfego sem identificadores pessoais. */
  private record Funnel(
      long humanEvents,
      long rawEvents,
      long visitors,
      long sessions,
      long rawSessions,
      long pageViews,
      long checkouts,
      Instant lastEvent) {}

  /** Responsabilidade: conferir identidade financeira, atribuição e evidência mínima de entrega. */
  private record Payment(
      String reference,
      BigDecimal amount,
      String currency,
      String status,
      boolean delivered,
      boolean checkoutAccess,
      boolean financialOutcome,
      boolean undatedPayment,
      String attribution) {}
}
