package com.marketinghub.repository.jdbc.facebookads;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Responsabilidade: consultar compras elegíveis e persistir confirmações idempotentes de conversão
 * da Meta no banco canônico.
 */
@Repository
public class FacebookPixelConversionJdbcRepository {

  private static final String PDE_PAYMENT_SOURCE = "PDE_PAYMENT";

  private final JdbcTemplate jdbcTemplate;

  /** Configura o acesso JDBC centralizado do módulo de pixel. */
  public FacebookPixelConversionJdbcRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Consulta compras aprovadas do Lead Portal que ainda não foram confirmadas no pixel. */
  public List<LegacyPixelConversionRow> findPendingLegacyConversions(int limit) {
    String sql =
        """
        SELECT
            p.id AS purchase_id,
            p.mp_payment_id,
            p.amount,
            p.currency,
            p.payment_approved_at,
            p.created_at,
            exp.id AS experiment_id,
            exp.name AS experiment_name,
            mn.facebook_pixel_id AS niche_pixel_id,
            SHA2(LOWER(TRIM(p.buyer_email)), 256) AS buyer_email_hash,
            exp.follow_up_action_url AS event_source_url
        FROM lead_portal_purchase p
        JOIN flow_submission_image_package pack ON pack.payment_purchase_id = p.id
        JOIN flow_submissions sub ON sub.id = pack.submission_id
        JOIN lead_portal_flow flow ON flow.slug = sub.flow_slug
        JOIN experiment exp ON exp.lead_portal_flow_id = flow.id
        JOIN market_niche mn ON mn.id = exp.niche_id
        WHERE p.status = 'APPROVED'
          AND p.pixel_conversion_recorded_at IS NULL
          AND mn.facebook_pixel_id IS NOT NULL
        ORDER BY p.created_at ASC
        LIMIT ?
        """;
    return jdbcTemplate.query(
        sql, ps -> ps.setInt(1, limit), (rs, rowNum) -> mapLegacyConversion(rs));
  }

  /** Consulta pagamentos PDE aprovados que ainda não têm confirmação CAPI. */
  public List<PdePixelConversionRow> findPendingPdeConversions(int limit) {
    String sql =
        """
        SELECT
            CONCAT(pa.provider, ':', pa.transaction_id) AS source_reference,
            CONCAT('pde:', pa.provider, ':', pa.transaction_id) AS meta_event_id,
            exp.id AS experiment_id,
            exp.name AS experiment_name,
            mn.facebook_pixel_id AS niche_pixel_id,
            pa.amount_cents,
            pa.currency,
            pa.verified_at,
            pa.buyer_reference_hash,
            slot.public_url AS event_source_url
        FROM pde_payment_audit pa
        JOIN pde_production_slot slot
          ON slot.product_slug = pa.product_slug
         AND slot.experience_version = pa.experience_version
         AND slot.source_experiment_id IS NOT NULL
         AND slot.status IN ('READY', 'ACTIVE')
        JOIN experiment exp ON exp.id = slot.source_experiment_id
        JOIN market_niche mn ON mn.id = exp.niche_id
        LEFT JOIN facebook_pixel_conversion_delivery delivery
          ON delivery.source_type = 'PDE_PAYMENT'
         AND delivery.source_reference = CONCAT(pa.provider, ':', pa.transaction_id)
        WHERE UPPER(pa.provider) = 'MERCADO_PAGO'
          AND LOWER(pa.payment_status) = 'approved'
          AND exp.platform = 'FACEBOOK'
          AND exp.campaign_objective = 'SALES'
          AND mn.facebook_pixel_id IS NOT NULL
          AND delivery.id IS NULL
        ORDER BY pa.verified_at ASC, pa.id ASC
        LIMIT ?
        """;
    return jdbcTemplate.query(
        sql,
        ps -> ps.setInt(1, limit),
        (rs, rowNum) ->
            new PdePixelConversionRow(
                rs.getString("source_reference"),
                rs.getLong("experiment_id"),
                rs.getString("experiment_name"),
                rs.getString("niche_pixel_id"),
                rs.getString("meta_event_id"),
                BigDecimal.valueOf(rs.getLong("amount_cents"), 2),
                rs.getString("currency"),
                toInstant(rs.getTimestamp("verified_at"), null),
                rs.getString("buyer_reference_hash"),
                rs.getString("event_source_url")));
  }

  /** Marca uma compra legada como entregue depois da confirmação da Meta. */
  public void markLegacyConversionRecorded(long purchaseId) {
    jdbcTemplate.update(
        "UPDATE lead_portal_purchase SET pixel_conversion_recorded_at = UTC_TIMESTAMP() WHERE id = ?",
        purchaseId);
  }

  /** Persiste uma confirmação PDE apenas se compra, slot, experimento e pixel ainda coincidirem. */
  public int insertPdeDeliveryIfEligible(String sourceReference, String pixelId) {
    return jdbcTemplate.update(
        """
        INSERT IGNORE INTO facebook_pixel_conversion_delivery
          (source_type, source_reference, pixel_id, meta_event_id, recorded_at)
        SELECT ?, ?, mn.facebook_pixel_id,
               CONCAT('pde:', pa.provider, ':', pa.transaction_id), CURRENT_TIMESTAMP
        FROM pde_payment_audit pa
        JOIN pde_production_slot slot
          ON slot.product_slug = pa.product_slug
         AND slot.experience_version = pa.experience_version
         AND slot.source_experiment_id IS NOT NULL
         AND slot.status IN ('READY', 'ACTIVE')
        JOIN experiment exp ON exp.id = slot.source_experiment_id
        JOIN market_niche mn ON mn.id = exp.niche_id
        WHERE CONCAT(pa.provider, ':', pa.transaction_id) = ?
          AND UPPER(pa.provider) = 'MERCADO_PAGO'
          AND LOWER(pa.payment_status) = 'approved'
          AND exp.platform = 'FACEBOOK'
          AND exp.campaign_objective = 'SALES'
          AND mn.facebook_pixel_id = ?
        LIMIT 1
        """,
        PDE_PAYMENT_SOURCE,
        sourceReference,
        sourceReference,
        pixelId);
  }

  /** Confirma se a mesma origem já foi registrada para o mesmo pixel. */
  public boolean pdeDeliveryExists(String sourceReference, String pixelId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM facebook_pixel_conversion_delivery
            WHERE source_type = ? AND source_reference = ? AND pixel_id = ?
            """,
            Integer.class,
            PDE_PAYMENT_SOURCE,
            sourceReference,
            pixelId);
    return count != null && count > 0;
  }

  /** Reconstrói a linha legada sem devolver o e-mail em texto puro. */
  private LegacyPixelConversionRow mapLegacyConversion(ResultSet rs) throws SQLException {
    return new LegacyPixelConversionRow(
        rs.getLong("purchase_id"),
        (Long) rs.getObject("experiment_id"),
        rs.getString("experiment_name"),
        rs.getString("niche_pixel_id"),
        rs.getString("mp_payment_id"),
        rs.getBigDecimal("amount"),
        rs.getString("currency"),
        toInstant(rs.getTimestamp("payment_approved_at"), rs.getTimestamp("created_at")),
        rs.getString("buyer_email_hash"),
        rs.getString("event_source_url"));
  }

  /** Usa o instante aprovado e recorre à criação apenas no contrato legado. */
  private Instant toInstant(Timestamp timestamp, Timestamp fallback) {
    if (timestamp != null) {
      return timestamp.toInstant();
    }
    return fallback != null ? fallback.toInstant() : null;
  }

  /** Representa a projeção JDBC de uma compra legada. */
  public record LegacyPixelConversionRow(
      Long purchaseId,
      Long experimentId,
      String experimentName,
      String pixelId,
      String paymentId,
      BigDecimal amount,
      String currency,
      Instant paymentApprovedAt,
      String hashedEmail,
      String eventSourceUrl) {}

  /** Representa a projeção JDBC de uma compra PDE. */
  public record PdePixelConversionRow(
      String sourceReference,
      Long experimentId,
      String experimentName,
      String pixelId,
      String eventId,
      BigDecimal amount,
      String currency,
      Instant paymentApprovedAt,
      String hashedEmail,
      String eventSourceUrl) {}
}
