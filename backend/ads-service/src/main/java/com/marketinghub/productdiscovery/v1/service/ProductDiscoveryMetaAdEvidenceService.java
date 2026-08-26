package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Solicita e entrega a Argos evidências Meta auditáveis sem expor credenciais. */
@Service
public class ProductDiscoveryMetaAdEvidenceService {
  private static final Logger log =
      LoggerFactory.getLogger(ProductDiscoveryMetaAdEvidenceService.class);
  private static final String WORKSPACE_ID = "workspace-001";
  private static final int MIN_LONGEVITY_DAYS = 30;
  private static final int MIN_OBSERVATIONS = 2;
  private static final Duration MAX_EVIDENCE_AGE = Duration.ofDays(30);

  private final JdbcTemplate jdbcTemplate;
  private final MoisMetaAdInvestigationService investigationService;
  private final ObjectMapper objectMapper;

  /** Inicializa a integração do domínio de descoberta com o radar Meta canônico. */
  public ProductDiscoveryMetaAdEvidenceService(
      JdbcTemplate jdbcTemplate,
      MoisMetaAdInvestigationService investigationService,
      ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.investigationService = investigationService;
    this.objectMapper = objectMapper;
  }

  /** Cria ou reutiliza o acompanhamento da categoria e devolve sua cobertura atual. */
  public ProductDiscoveryMetaAdEvidenceListResponse requestAndSearch(
      Long cycleId, ProductDiscoveryMetaAdEvidenceRequest request) {
    String query = normalizedQuery(request.query());
    String country = normalizedCountry(request.country());
    String publisherPlatform = normalizedPublisherPlatform(request.publisherPlatform());
    if (specificTerms(query).size() < 2) {
      throw new IllegalArgumentException(
          "A consulta Meta deve conter ao menos dois termos específicos da categoria");
    }
    MoisMetaAdDtos.InvestigationResponse investigation =
        investigationService.ensureForProductDiscovery(
            WORKSPACE_ID, query, country, publisherPlatform);
    log.info(
        "Product Discovery solicitou evidência Meta cycleId={} investigationId={} country={} publisherPlatform={} query={}",
        cycleId,
        investigation.id(),
        country,
        publisherPlatform,
        query);
    return searchInternal(
        cycleId, query, country, publisherPlatform, request.limit(), investigation);
  }

  /** Pesquisa somente evidências já persistidas, sem criar acompanhamento por efeito de um GET. */
  public ProductDiscoveryMetaAdEvidenceListResponse searchExisting(
      String query, String country, String publisherPlatform, Integer limit) {
    return searchInternal(
        null,
        normalizedQuery(query),
        normalizedCountry(country),
        normalizedPublisherPlatform(publisherPlatform),
        limit,
        null);
  }

  /** Filtra país, termos e plataforma antes de calcular a cobertura comercial conservadora. */
  private ProductDiscoveryMetaAdEvidenceListResponse searchInternal(
      Long cycleId,
      String query,
      String country,
      String publisherPlatform,
      Integer limit,
      MoisMetaAdDtos.InvestigationResponse investigation) {
    int normalizedLimit = Math.max(1, Math.min(limit == null ? 25 : limit, 50));
    List<String> terms = specificTerms(query);
    String filter =
        terms.isEmpty()
            ? " AND 1 = 0"
            : " AND ("
                + String.join(
                    " OR ",
                    java.util.Collections.nCopies(
                        terms.size(),
                        "LOWER(CONCAT_WS(' ', a.advertiser_name, a.ad_texts_json, a.destination_url)) LIKE ?"))
                + ")";
    ArrayList<Object> parameters = new ArrayList<>();
    parameters.add(country);
    terms.forEach(term -> parameters.add("%" + term + "%"));
    parameters.add("%" + publisherPlatform.toLowerCase(Locale.ROOT) + "%");
    parameters.add(normalizedLimit);
    List<ProductDiscoveryMetaAdEvidenceResponse> items =
        jdbcTemplate.query(
            """
            SELECT a.meta_ad_id, a.advertiser_name, a.ad_texts_json,
                   a.publisher_platforms_json, a.format_types_json, a.destination_url,
                   a.snapshot_url, a.ad_status, a.page_active, a.commercial_signal,
                   a.observation_count, a.first_observed_at, a.last_observed_at
            FROM mois_meta_ad_asset a
            WHERE a.workspace_id = 'workspace-001'
              AND EXISTS (
                SELECT 1 FROM mois_meta_ad_observation o
                JOIN mois_meta_ad_investigation i ON i.id = o.investigation_id
                WHERE o.asset_id = a.id AND i.country_code = ?)
            """
                + filter
                + " AND LOWER(COALESCE(a.publisher_platforms_json, '')) LIKE ?"
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
                  stringList(
                      rs.getString("ad_texts_json"), "ad_texts_json", rs.getString("meta_ad_id")),
                  stringList(
                      rs.getString("publisher_platforms_json"),
                      "publisher_platforms_json",
                      rs.getString("meta_ad_id")),
                  stringList(
                      rs.getString("format_types_json"),
                      "format_types_json",
                      rs.getString("meta_ad_id")),
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
    Instant now = Instant.now();
    Instant latestObservationAt =
        items.stream()
            .map(ProductDiscoveryMetaAdEvidenceResponse::lastObservedAt)
            .max(Instant::compareTo)
            .orElse(null);
    int activeAds =
        (int)
            items.stream()
                .filter(ProductDiscoveryMetaAdEvidenceResponse::active)
                .filter(item -> !item.lastObservedAt().isBefore(now.minus(MAX_EVIDENCE_AGE)))
                .count();
    int advertisers =
        (int)
            items.stream()
                .map(ProductDiscoveryMetaAdEvidenceResponse::advertiserName)
                .filter(StringUtils::hasText)
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .count();
    String sourceStatus = sourceStatus(investigation, items, activeAds, latestObservationAt, now);
    return new ProductDiscoveryMetaAdEvidenceListResponse(
        cycleId,
        query,
        country,
        publisherPlatform,
        sourceStatus,
        investigation == null ? "PERSISTED_ONLY" : investigation.collection().mode(),
        investigation == null ? null : investigation.id(),
        investigation == null ? null : investigation.collection().searchUrl(),
        items.size(),
        activeAds,
        advertisers,
        latestObservationAt,
        interpretation(sourceStatus, items.size(), activeAds, advertisers),
        items);
  }

  /** Distingue ausência observada, falta de coleta, baixa aderência e evidência desatualizada. */
  private String sourceStatus(
      MoisMetaAdDtos.InvestigationResponse investigation,
      List<ProductDiscoveryMetaAdEvidenceResponse> items,
      int activeAds,
      Instant latestObservationAt,
      Instant now) {
    if (!items.isEmpty()
        && latestObservationAt != null
        && latestObservationAt.isBefore(now.minus(MAX_EVIDENCE_AGE))) {
      return "STALE";
    }
    if (activeAds > 0) {
      return "OBSERVED";
    }
    if (!items.isEmpty()) {
      return "NO_ACTIVE_ADS";
    }
    if (investigation == null) {
      return "NOT_REQUESTED";
    }
    if (investigation.adsObserved() > 0) {
      return "NO_RELEVANT_PLATFORM_EVIDENCE";
    }
    return "OFFICIAL_API".equals(investigation.collection().mode())
        ? "AWAITING_OFFICIAL_COLLECTION"
        : "AWAITING_SUPERVISED_OBSERVATION";
  }

  /** Explica as métricas sem converter presença publicitária em venda comprovada. */
  private String interpretation(
      String sourceStatus, int adsObserved, int activeAds, int advertisersObserved) {
    return "Cobertura "
        + sourceStatus
        + ": "
        + adsObserved
        + " anúncio(s) aderente(s), "
        + activeAds
        + " ativo(s) e "
        + advertisersObserved
        + " anunciante(s). Presença, atividade e longevidade indicam investimento; não comprovam vendas.";
  }

  /** Mantém no filtro somente termos específicos para evitar coincidências genéricas. */
  private List<String> specificTerms(String query) {
    List<String> ignored =
        List.of(
            "como",
            "para",
            "pela",
            "pelo",
            "brasil",
            "consumidor",
            "instagram",
            "produto",
            "produtos",
            "digital",
            "digitais",
            "anuncio",
            "anuncios");
    return Arrays.stream(query.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
        .filter(term -> term.length() >= 4)
        .filter(term -> !ignored.contains(term))
        .distinct()
        .limit(6)
        .toList();
  }

  /** Normaliza a consulta antes de persistir ou comparar investigações. */
  private String normalizedQuery(String query) {
    return StringUtils.hasText(query) ? query.trim() : "";
  }

  /** Normaliza o território conforme o contrato da Graph API. */
  private String normalizedCountry(String country) {
    return StringUtils.hasText(country) ? country.trim().toUpperCase(Locale.ROOT) : "BR";
  }

  /** Limita a descoberta ao Instagram quando a plataforma não vier informada. */
  private String normalizedPublisherPlatform(String publisherPlatform) {
    String normalized =
        StringUtils.hasText(publisherPlatform)
            ? publisherPlatform.trim().toUpperCase(Locale.ROOT)
            : "INSTAGRAM";
    if (!"INSTAGRAM".equals(normalized)) {
      throw new IllegalArgumentException(
          "A descoberta B2C v1 aceita somente evidências do Instagram");
    }
    return normalized;
  }

  /** Desserializa listas persistidas para impedir JSON textual dentro do contrato de Argos. */
  private List<String> stringList(String payload, String field, String metaAdId) {
    if (!StringUtils.hasText(payload)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(payload, new TypeReference<>() {});
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao ler evidência Meta metaAdId={} field={} payload={}",
          metaAdId,
          field,
          payload,
          ex);
      throw new IllegalStateException("Evidência Meta persistida possui JSON inválido", ex);
    }
  }
}
