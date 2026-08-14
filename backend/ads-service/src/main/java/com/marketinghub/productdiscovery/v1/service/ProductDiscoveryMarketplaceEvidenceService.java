package com.marketinghub.productdiscovery.v1.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Consulta snapshots autenticados sem expor credenciais ou acoplar Argos aos coletores. */
@Service
public class ProductDiscoveryMarketplaceEvidenceService {
  private final JdbcTemplate jdbcTemplate;

  /** Inicializa a leitura auditavel dos snapshots persistidos pelo backend. */
  public ProductDiscoveryMarketplaceEvidenceService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Lista ofertas da coleta mais recente filtradas pela pergunta dirigida de Argos. */
  public ProductDiscoveryMarketplaceOfferListResponse search(
      String marketplace, String query, Integer limit) {
    String source = normalizeMarketplace(marketplace);
    String normalizedQuery = StringUtils.hasText(query) ? query.trim() : "";
    int normalizedLimit = Math.max(1, Math.min(limit == null ? 10 : limit, 25));
    String jobId = latestJobId(source);
    if (jobId == null) {
      return new ProductDiscoveryMarketplaceOfferListResponse(
          source, normalizedQuery, null, List.of());
    }
    List<String> terms =
        Arrays.stream(normalizedQuery.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
            .filter(term -> term.length() >= 4)
            .distinct()
            .limit(6)
            .toList();
    String relevanceFilter =
        terms.isEmpty()
            ? ""
            : " AND ("
                + String.join(
                    " OR ",
                    java.util.Collections.nCopies(
                        terms.size(),
                        "LOWER(CONCAT_WS(' ', product_name, title, hotmart_description)) LIKE ?"))
                + ")";
    List<Object> parameters = new ArrayList<>();
    parameters.add(source);
    parameters.add(jobId);
    terms.forEach(term -> parameters.add("%" + term + "%"));
    parameters.add(normalizedLimit);
    List<ProductDiscoveryMarketplaceOfferResponse> items =
        jdbcTemplate.query(
            """
            SELECT reference_id, COALESCE(NULLIF(product_name, ''), title) AS offer_title,
                   COALESCE(NULLIF(product_url, ''), sales_page_url) AS offer_url,
                   hotmart_description, producer_name, hotmart_price, hotmart_temperature,
                   success_score, collected_at
            FROM mois_collected_reference
            WHERE workspace_id = 'workspace-001' AND source = ? AND job_id = ?
            """
                + relevanceFilter
                + """
            ORDER BY COALESCE(hotmart_temperature, success_score, 0) DESC, collected_at DESC
            LIMIT ?
            """,
            (rs, rowNum) -> mapOffer(source, rs),
            parameters.toArray());
    return new ProductDiscoveryMarketplaceOfferListResponse(source, normalizedQuery, jobId, items);
  }

  /** Localiza a ultima coleta concluida do marketplace solicitado. */
  private String latestJobId(String source) {
    return jdbcTemplate
        .query(
            """
            SELECT job_id FROM mois_collected_reference
            WHERE workspace_id = 'workspace-001' AND source = ?
            ORDER BY updated_at DESC LIMIT 1
            """,
            (rs, rowNum) -> rs.getString("job_id"),
            source)
        .stream()
        .findFirst()
        .orElse(null);
  }

  /** Converte o snapshot relacional no contrato comum consumido por Argos. */
  private ProductDiscoveryMarketplaceOfferResponse mapOffer(String source, ResultSet rs)
      throws SQLException {
    Timestamp collectedAt = rs.getTimestamp("collected_at");
    Double traction =
        rs.getObject("hotmart_temperature") != null
            ? rs.getDouble("hotmart_temperature")
            : rs.getObject("success_score") != null ? rs.getDouble("success_score") : null;
    return new ProductDiscoveryMarketplaceOfferResponse(
        source,
        rs.getString("reference_id"),
        rs.getString("offer_title"),
        rs.getString("offer_url"),
        rs.getString("hotmart_description"),
        rs.getString("producer_name"),
        rs.getString("hotmart_price"),
        traction,
        collectedAt == null ? null : collectedAt.toInstant());
  }

  /** Restringe a pesquisa aos dois marketplaces autenticados permitidos. */
  private String normalizeMarketplace(String marketplace) {
    String source =
        StringUtils.hasText(marketplace) ? marketplace.trim().toUpperCase(Locale.ROOT) : "";
    if (!List.of("HOTMART", "CLICKBANK").contains(source)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marketplace nao autorizado");
    }
    return source;
  }
}
