package com.marketinghub.productdiscovery.v1.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Consulta snapshots autenticados sem expor credenciais ou acoplar Argos aos coletores. */
@Service
public class ProductDiscoveryMarketplaceEvidenceService {
  private static final List<String> GENERIC_RELEVANCE_TERMS =
      List.of(
          "como",
          "para",
          "pela",
          "pelo",
          "prestador",
          "prestadores",
          "servico",
          "servicos",
          "cliente",
          "clientes",
          "pequeno",
          "pequenos",
          "local",
          "locais",
          "brasil",
          "whatsapp",
          "oferta",
          "ofertas",
          "comercial",
          "comerciais",
          "preco",
          "venda",
          "vendas",
          "produto",
          "produtos",
          "digital",
          "digitais",
          "execucao",
          "validada",
          "validado",
          "claras",
          "claro",
          "pde");
  private final JdbcTemplate jdbcTemplate;

  /** Inicializa a leitura auditavel dos snapshots persistidos pelo backend. */
  public ProductDiscoveryMarketplaceEvidenceService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Lista ofertas históricas filtradas pela pergunta dirigida de Argos. */
  public ProductDiscoveryMarketplaceOfferListResponse search(
      String marketplace, String query, Integer limit) {
    String source = normalizeMarketplace(marketplace);
    String normalizedQuery = StringUtils.hasText(query) ? query.trim() : "";
    int normalizedLimit = Math.max(1, Math.min(limit == null ? 10 : limit, 25));
    List<String> terms =
        Arrays.stream(normalizedQuery.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
            .filter(term -> term.length() >= 4)
            .map(this::normalizeTerm)
            .filter(term -> !GENERIC_RELEVANCE_TERMS.contains(term))
            .distinct()
            .limit(6)
            .toList();
    String relevanceFilter =
        terms.isEmpty()
            ? ""
            : " AND ("
                + String.join(
                    " + ",
                    java.util.Collections.nCopies(
                        terms.size(),
                        "CASE WHEN LOWER(CONCAT_WS(' ', product_name, title, hotmart_description)) LIKE ? THEN 1 ELSE 0 END"))
                + ") >= ?\n";
    List<Object> parameters = new ArrayList<>();
    parameters.add(source);
    terms.forEach(term -> parameters.add("%" + term + "%"));
    if (!terms.isEmpty()) parameters.add(Math.min(2, terms.size()));
    parameters.add(250);
    List<MarketplaceHistoryRow> rows =
        jdbcTemplate.query(
            """
            SELECT job_id, reference_id, COALESCE(NULLIF(product_name, ''), title) AS offer_title,
                   COALESCE(NULLIF(product_url, ''), sales_page_url) AS offer_url,
                   hotmart_description, producer_name, hotmart_price, hotmart_temperature,
                   success_score, hotmart_rating, hotmart_review_count, hotmart_blueprint,
                   hotmart_commission, hotmart_category, hotmart_format, ranking_position, collected_at
            FROM mois_collected_reference
            WHERE workspace_id = 'workspace-001' AND source = ?
            """
                + relevanceFilter
                + """
            ORDER BY collected_at DESC, COALESCE(hotmart_temperature, 0) DESC
            LIMIT ?
            """,
            (rs, rowNum) -> mapHistoryRow(rs),
            parameters.toArray());
    Map<String, List<MarketplaceHistoryRow>> histories = new LinkedHashMap<>();
    rows.forEach(
        row -> histories.computeIfAbsent(row.referenceId(), ignored -> new ArrayList<>()).add(row));
    List<ProductDiscoveryMarketplaceOfferResponse> items =
        histories.values().stream()
            .map(history -> mapOffer(source, history))
            .filter(item -> item.productUrl() != null && !item.productUrl().isBlank())
            .limit(normalizedLimit)
            .toList();
    String newestJobId = rows.isEmpty() ? null : rows.getFirst().jobId();
    return new ProductDiscoveryMarketplaceOfferListResponse(
        source, normalizedQuery, newestJobId, items);
  }

  /** Converte uma linha relacional em observação histórica de marketplace. */
  private MarketplaceHistoryRow mapHistoryRow(ResultSet rs) throws SQLException {
    Timestamp collectedAt = rs.getTimestamp("collected_at");
    Double traction =
        rs.getObject("hotmart_temperature") != null
            ? rs.getDouble("hotmart_temperature")
            : rs.getObject("success_score") != null ? rs.getDouble("success_score") : null;
    return new MarketplaceHistoryRow(
        rs.getString("job_id"),
        rs.getString("reference_id"),
        rs.getString("offer_title"),
        rs.getString("offer_url"),
        rs.getString("hotmart_description"),
        rs.getString("producer_name"),
        rs.getString("hotmart_price"),
        traction,
        nullableDouble(rs, "hotmart_rating"),
        nullableInteger(rs, "hotmart_review_count"),
        nullableDouble(rs, "hotmart_blueprint"),
        rs.getString("hotmart_commission"),
        rs.getString("hotmart_category"),
        rs.getString("hotmart_format"),
        nullableInteger(rs, "ranking_position"),
        collectedAt == null ? null : collectedAt.toInstant());
  }

  /** Consolida observações do mesmo produto sem confundir tração com venda comprovada. */
  private ProductDiscoveryMarketplaceOfferResponse mapOffer(
      String source, List<MarketplaceHistoryRow> history) {
    MarketplaceHistoryRow current = history.getFirst();
    MarketplaceHistoryRow previous = history.size() > 1 ? history.get(1) : null;
    MarketplaceHistoryRow oldest = history.getLast();
    int completeness = 0;
    if (current.price() != null) completeness++;
    if (current.traction() != null) completeness++;
    if (current.rating() != null) completeness++;
    if (current.reviewCount() != null) completeness++;
    if (current.blueprint() != null) completeness++;
    if (StringUtils.hasText(current.commission())) completeness++;
    String confidence =
        history.size() >= 2 && completeness >= 5 ? "HIGH" : completeness >= 3 ? "MEDIUM" : "LOW";
    return new ProductDiscoveryMarketplaceOfferResponse(
        source,
        current.referenceId(),
        current.title(),
        current.url(),
        current.description(),
        current.producer(),
        current.price(),
        current.traction(),
        current.rating(),
        current.reviewCount(),
        current.blueprint(),
        current.commission(),
        current.category(),
        current.format(),
        current.rankingPosition(),
        history.size(),
        previous == null ? null : previous.traction(),
        oldest.collectedAt(),
        current.collectedAt(),
        confidence);
  }

  /** Lê decimal anulável sem converter ausência em zero. */
  private Double nullableDouble(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column) == null ? null : rs.getDouble(column);
  }

  /** Lê inteiro anulável sem converter ausência em zero. */
  private Integer nullableInteger(ResultSet rs, String column) throws SQLException {
    return rs.getObject(column) == null ? null : rs.getInt(column);
  }

  /** Remove acentos para comparar termos genéricos sem depender da collation do banco. */
  private String normalizeTerm(String value) {
    return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "");
  }

  /** Representa uma observação de oferta em uma coleta específica. */
  private record MarketplaceHistoryRow(
      String jobId,
      String referenceId,
      String title,
      String url,
      String description,
      String producer,
      String price,
      Double traction,
      Double rating,
      Integer reviewCount,
      Double blueprint,
      String commission,
      String category,
      String format,
      Integer rankingPosition,
      Instant collectedAt) {}

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
