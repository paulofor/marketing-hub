package com.marketinghub.productdiscovery.v1.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Entrega a Argos anúncios Meta já coletados sem expor token nem afirmar vendas inexistentes. */
@Service
public class ProductDiscoveryMetaAdEvidenceService {
  private static final int MIN_LONGEVITY_DAYS = 30;
  private static final int MIN_OBSERVATIONS = 2;

  private final JdbcTemplate jdbcTemplate;

  /** Inicializa a consulta somente leitura das evidências persistidas pelo coletor Meta. */
  public ProductDiscoveryMetaAdEvidenceService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Pesquisa anúncios reais e calcula um sinal conservador de investimento sustentado. */
  public ProductDiscoveryMetaAdEvidenceListResponse search(
      String query, String country, Integer limit) {
    String normalizedQuery = StringUtils.hasText(query) ? query.trim() : "";
    String normalizedCountry =
        StringUtils.hasText(country) ? country.trim().toUpperCase(Locale.ROOT) : "BR";
    int normalizedLimit = Math.max(1, Math.min(limit == null ? 25 : limit, 50));
    List<String> terms =
        Arrays.stream(normalizedQuery.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
            .filter(term -> term.length() >= 4)
            .distinct()
            .limit(6)
            .toList();
    String filter =
        terms.isEmpty()
            ? ""
            : " AND ("
                + String.join(
                    " OR ",
                    java.util.Collections.nCopies(
                        terms.size(),
                        "LOWER(CONCAT_WS(' ', a.advertiser_name, a.ad_texts_json, a.destination_url)) LIKE ?"))
                + ")";
    java.util.ArrayList<Object> parameters = new java.util.ArrayList<>();
    parameters.add(normalizedCountry);
    terms.forEach(term -> parameters.add("%" + term + "%"));
    parameters.add(normalizedLimit);
    List<ProductDiscoveryMetaAdEvidenceResponse> items =
        jdbcTemplate.query(
            """
            SELECT a.meta_ad_id, a.advertiser_name, a.ad_texts_json, a.format_types_json,
                   a.destination_url, a.snapshot_url, a.ad_status, a.page_active,
                   a.commercial_signal, a.observation_count, a.first_observed_at,
                   a.last_observed_at
            FROM mois_meta_ad_asset a
            WHERE a.workspace_id = 'workspace-001'
              AND EXISTS (
                SELECT 1 FROM mois_meta_ad_observation o
                JOIN mois_meta_ad_investigation i ON i.id = o.investigation_id
                WHERE o.asset_id = a.id AND i.country_code = ?)
            """
                + filter
                + " ORDER BY a.page_active DESC, a.last_observed_at DESC LIMIT ?",
            (rs, rowNum) -> {
              Instant first = rs.getTimestamp("first_observed_at").toInstant();
              Instant last = rs.getTimestamp("last_observed_at").toInstant();
              long longevity = Math.max(0, Duration.between(first, last).toDays());
              int observations = rs.getInt("observation_count");
              boolean active =
                  rs.getBoolean("page_active")
                      && "ACTIVE".equalsIgnoreCase(rs.getString("ad_status"));
              boolean commercial = rs.getBoolean("commercial_signal");
              boolean sustained =
                  active
                      && commercial
                      && observations >= MIN_OBSERVATIONS
                      && longevity >= MIN_LONGEVITY_DAYS;
              String confidence = sustained ? "HIGH" : observations >= 2 ? "MEDIUM" : "LOW";
              return new ProductDiscoveryMetaAdEvidenceResponse(
                  rs.getString("meta_ad_id"),
                  rs.getString("advertiser_name"),
                  rs.getString("ad_texts_json"),
                  rs.getString("format_types_json"),
                  rs.getString("destination_url"),
                  rs.getString("snapshot_url"),
                  active,
                  commercial,
                  observations,
                  longevity,
                  sustained,
                  confidence,
                  first,
                  last);
            },
            parameters.toArray());
    return new ProductDiscoveryMetaAdEvidenceListResponse(
        normalizedQuery,
        normalizedCountry,
        "Anúncio ativo e longevo indica investimento sustentado, mas não comprova vendas isoladamente.",
        items);
  }
}
