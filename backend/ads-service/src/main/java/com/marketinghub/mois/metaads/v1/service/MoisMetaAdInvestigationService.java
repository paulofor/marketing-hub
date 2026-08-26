package com.marketinghub.mois.metaads.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persiste observações reais e decide se um padrão Meta pode ser investigado, modelado ou
 * descartado.
 */
@Service
@RequiredArgsConstructor
public class MoisMetaAdInvestigationService {

  private static final Logger log = LoggerFactory.getLogger(MoisMetaAdInvestigationService.class);
  private static final int MIN_LONGEVITY_DAYS = 30;
  private static final int MIN_OBSERVATIONS = 2;
  private static final int MIN_VARIATIONS = 3;
  private static final String SUPERVISED_COLLECTION_REASON =
      "A API oficial da Meta não disponibiliza anúncios comerciais gerais que não alcançaram a União Europeia; no Brasil, a observação deve ser supervisionada.";
  private static final String OFFICIAL_API_COLLECTION_REASON =
      "Território coberto para anúncios comerciais pela API oficial; a execução depende de preflight real da autorização do aplicativo.";
  private static final Set<String> OFFICIAL_COMMERCIAL_API_COUNTRIES =
      Set.of(
          "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE", "IT",
          "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE", "GB");

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /** Cria um acompanhamento no modo compatível com a cobertura comercial do território. */
  public MoisMetaAdDtos.InvestigationResponse create(
      MoisMetaAdDtos.CreateInvestigationRequest request) {
    Instant now = Instant.now();
    String country = normalizedCountry(request.countryCode());
    String publisherPlatform = normalizedPublisherPlatform(request.publisherPlatform());
    boolean officialApi = supportsOfficialCommercialApi(country);
    String status = officialApi ? "PENDING" : "ACTIVE_SUPERVISED";
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  """
                  INSERT INTO mois_meta_ad_investigation
                    (workspace_id, search_terms, country_code, publisher_platform, status,
                     gate_decision, evidence_json, gaps_json, ethical_modeling_json, created_at,
                     updated_at)
                  VALUES (?, ?, ?, ?, ?, 'INVESTIGAR', '[]', ?, ?, ?, ?)
                  """,
                  java.sql.Statement.RETURN_GENERATED_KEYS);
          statement.setString(1, request.workspaceId());
          statement.setString(2, request.searchTerms().trim());
          statement.setString(3, country);
          statement.setString(4, publisherPlatform);
          statement.setString(5, status);
          statement.setString(
              6,
              json(
                  officialApi
                      ? List.of(
                          "Concluir o preflight da autorização oficial da Meta",
                          "Coletar a primeira observação real na plataforma solicitada")
                      : List.of(
                          "Registrar a primeira observação real pela Biblioteca pública da Meta",
                          "Reobservar o mesmo anúncio em datas distintas para comprovar longevidade")));
          statement.setString(7, json(MoisMetaAdDtos.EthicalModelingCard.empty()));
          statement.setTimestamp(8, Timestamp.from(now));
          statement.setTimestamp(9, Timestamp.from(now));
          return statement;
        },
        keyHolder);
    Number id = keyHolder.getKey();
    return get(id == null ? 0 : id.longValue()).orElseThrow();
  }

  /** Reutiliza ou cria de forma idempotente o acompanhamento solicitado por Argos. */
  @Transactional
  public MoisMetaAdDtos.InvestigationResponse ensureForProductDiscovery(
      String workspaceId, String searchTerms, String countryCode, String publisherPlatform) {
    String normalizedCountry = normalizedCountry(countryCode);
    String normalizedPlatform = normalizedPublisherPlatform(publisherPlatform);
    String normalizedTerms = searchTerms == null ? "" : searchTerms.trim();
    List<Long> existing =
        jdbcTemplate.query(
            """
            SELECT id
            FROM mois_meta_ad_investigation
            WHERE workspace_id = ?
              AND country_code = ?
              AND publisher_platform = ?
              AND LOWER(TRIM(search_terms)) = LOWER(?)
            ORDER BY created_at DESC
            LIMIT 1
            """,
            (rs, rowNum) -> rs.getLong("id"),
            workspaceId,
            normalizedCountry,
            normalizedPlatform,
            normalizedTerms);
    if (!existing.isEmpty()) {
      return required(existing.getFirst());
    }
    return create(
        new MoisMetaAdDtos.CreateInvestigationRequest(
            workspaceId, normalizedTerms, normalizedCountry, normalizedPlatform));
  }

  /** Lista investigações recentes de um workspace. */
  public MoisMetaAdDtos.InvestigationListResponse list(String workspaceId) {
    List<Long> ids =
        jdbcTemplate.query(
            "SELECT id FROM mois_meta_ad_investigation WHERE workspace_id = ? ORDER BY created_at DESC",
            (rs, rowNum) -> rs.getLong("id"),
            workspaceId);
    return new MoisMetaAdDtos.InvestigationListResponse(ids.stream().map(this::required).toList());
  }

  /** Busca uma investigação com seus sinais persistidos. */
  public Optional<MoisMetaAdDtos.InvestigationResponse> get(long id) {
    return jdbcTemplate
        .query(
            """
            SELECT i.*,
                   (SELECT COUNT(*) FROM mois_meta_ad_observation o WHERE o.investigation_id = i.id) ads_observed,
                   (SELECT MIN(o.observed_at) FROM mois_meta_ad_observation o WHERE o.investigation_id = i.id) first_observed_at
            FROM mois_meta_ad_investigation i WHERE i.id = ?
            """,
            (rs, rowNum) ->
                new MoisMetaAdDtos.InvestigationResponse(
                    rs.getLong("id"),
                    rs.getString("workspace_id"),
                    rs.getString("search_terms"),
                    rs.getString("country_code"),
                    rs.getString("publisher_platform"),
                    rs.getString("status"),
                    collectionState(
                        rs.getString("country_code"),
                        rs.getString("publisher_platform"),
                        rs.getString("search_terms"),
                        rs.getTimestamp("first_observed_at") == null
                            ? null
                            : rs.getTimestamp("first_observed_at").toInstant(),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("next_run_at") == null
                            ? null
                            : rs.getTimestamp("next_run_at").toInstant()),
                    rs.getString("gate_decision"),
                    stringList(rs.getString("evidence_json")),
                    stringList(rs.getString("gaps_json")),
                    modelingCard(rs.getString("ethical_modeling_json")),
                    creativeBrief(rs.getString("creative_briefing_json")),
                    rs.getInt("ads_observed"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()),
            id)
        .stream()
        .findFirst();
  }

  /** Gera um briefing original somente depois que as evidências permitem modelagem ética. */
  public MoisMetaAdDtos.InvestigationResponse generateCreativeBrief(long id) {
    MoisMetaAdDtos.InvestigationResponse investigation = required(id);
    if (!"MODELAR".equals(investigation.gateDecision())) {
      throw new IllegalStateException(
          "Briefing bloqueado: a investigação ainda não possui evidência suficiente para MODELAR");
    }
    MoisMetaAdDtos.EthicalModelingCard card = investigation.ethicalModeling();
    Instant generatedAt = Instant.now();
    MoisMetaAdDtos.CreativeIntelligenceBrief brief =
        new MoisMetaAdDtos.CreativeIntelligenceBrief(
            "READY_FOR_AD_SPECIALIST",
            "Conceito original para " + investigation.searchTerms(),
            "Mostre a dor de "
                + card.pain()
                + " e abra uma possibilidade concreta sem repetir a redação observada.",
            "Crie uma cena própria que materialize "
                + firstOrFallback(card.patterns(), "a transformação")
                + ", com marca, personagem e composição inéditos.",
            "Apresente "
                + card.offerStructure()
                + " por um ângulo original de "
                + firstOrFallback(card.angles(), "demonstração"),
            "Convide a pessoa a conhecer a solução com uma ação específica e coerente com a oferta.",
            investigation.evidences(),
            "MEDIUM",
            true,
            generatedAt);
    jdbcTemplate.update(
        "UPDATE mois_meta_ad_investigation SET creative_briefing_json = ?, updated_at = ? WHERE id = ?",
        json(brief),
        Timestamp.from(generatedAt),
        id);
    return required(id);
  }

  /** Reserva a execução pendente mais antiga sem expor endpoints administrativos ao coletor. */
  @Transactional
  public Optional<MoisMetaAdDtos.PendingInvestigationResponse> claimPending() {
    List<MoisMetaAdDtos.PendingInvestigationResponse> pending =
        jdbcTemplate.query(
            """
            SELECT id, workspace_id, search_terms, country_code, publisher_platform
            FROM mois_meta_ad_investigation
            WHERE status = 'PENDING' OR (status = 'COMPLETED' AND next_run_at <= ?)
            ORDER BY COALESCE(next_run_at, created_at) LIMIT 1
            """,
            (rs, rowNum) ->
                new MoisMetaAdDtos.PendingInvestigationResponse(
                    rs.getLong("id"),
                    rs.getString("workspace_id"),
                    rs.getString("search_terms"),
                    rs.getString("country_code"),
                    rs.getString("publisher_platform")),
            Timestamp.from(Instant.now()));
    if (pending.isEmpty()) return Optional.empty();
    MoisMetaAdDtos.PendingInvestigationResponse item = pending.getFirst();
    int updated =
        jdbcTemplate.update(
            """
            UPDATE mois_meta_ad_investigation
            SET status = 'RUNNING', started_at = ?, updated_at = ?
            WHERE id = ? AND (status = 'PENDING' OR (status = 'COMPLETED' AND next_run_at <= ?))
            """,
            Timestamp.from(Instant.now()),
            Timestamp.from(Instant.now()),
            item.id(),
            Timestamp.from(Instant.now()));
    return updated == 1 ? Optional.of(item) : Optional.empty();
  }

  /** Persiste o payload bruto antes de consolidar os sinais de cada anúncio. */
  @Transactional
  public MoisMetaAdDtos.ObservationBatchResponse ingest(
      long investigationId, MoisMetaAdDtos.ObservationBatchRequest request) {
    MoisMetaAdDtos.InvestigationResponse investigation = required(investigationId);
    Instant observedAt = request.observedAt() == null ? Instant.now() : request.observedAt();
    int accepted = 0;
    for (MoisMetaAdDtos.MetaAdObservation observation : request.observations()) {
      log.info(
          "MOIS Meta ingestão bruta investigationId={} collectorRunId={} metaAdId={} payload={}",
          investigationId,
          request.collectorRunId(),
          observation.metaAdId(),
          observation.rawPayload());
      if (alreadyObserved(
          investigationId,
          request.collectorRunId(),
          investigation.workspaceId(),
          observation.metaAdId())) {
        continue;
      }
      long assetId = upsertAsset(investigation.workspaceId(), observation, observedAt);
      accepted +=
          jdbcTemplate.update(
              """
              INSERT IGNORE INTO mois_meta_ad_observation
                (investigation_id, asset_id, collector_run_id, observed_at, raw_payload_json, created_at)
              VALUES (?, ?, ?, ?, ?, ?)
              """,
              investigationId,
              assetId,
              request.collectorRunId(),
              Timestamp.from(observedAt),
              observation.rawPayload(),
              Timestamp.from(Instant.now()));
    }
    GateResult gate = calculateGate(investigation.workspaceId(), investigationId);
    persistGate(investigationId, gate);
    return new MoisMetaAdDtos.ObservationBatchResponse(
        investigationId, accepted, gate.decision(), gate.gaps());
  }

  /** Normaliza e persiste uma observação cadastrada pela tela administrativa. */
  public MoisMetaAdDtos.ObservationBatchResponse ingestSupervised(
      long investigationId, MoisMetaAdDtos.SupervisedObservationRequest request) {
    Instant observedAt = request.observedAt() == null ? Instant.now() : request.observedAt();
    MoisMetaAdDtos.MetaAdObservation observation =
        new MoisMetaAdDtos.MetaAdObservation(
            request.adReference().trim(),
            null,
            request.advertiserName().trim(),
            "ACTIVE",
            normalizePublisherPlatforms(
                request.publisherPlatforms(), required(investigationId).publisherPlatform()),
            request.formatType() == null || request.formatType().isBlank()
                ? List.of()
                : List.of(request.formatType().trim()),
            List.of(request.adText().trim()),
            request.mediaUrl() == null || request.mediaUrl().isBlank()
                ? List.of()
                : List.of(request.mediaUrl().trim()),
            blankToNull(request.destinationUrl()),
            request.adLibraryUrl().trim(),
            request.pageActive(),
            request.commercialSignal(),
            json(request));
    String collectorRunId = "supervised-" + observedAt.toString().replace(":", "").replace(".", "");
    MoisMetaAdDtos.ObservationBatchResponse response =
        ingest(
            investigationId,
            new MoisMetaAdDtos.ObservationBatchRequest(
                collectorRunId, List.of(observation), observedAt));
    jdbcTemplate.update(
        "UPDATE mois_meta_ad_investigation SET status = 'ACTIVE_SUPERVISED', error_message = NULL, next_run_at = NULL, updated_at = ? WHERE id = ?",
        Timestamp.from(Instant.now()),
        investigationId);
    return response;
  }

  /** Impede que retry do mesmo lote infle artificialmente a contagem temporal. */
  private boolean alreadyObserved(
      long investigationId, String collectorRunId, String workspaceId, String metaAdId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM mois_meta_ad_observation o
            JOIN mois_meta_ad_asset a ON a.id = o.asset_id
            WHERE o.investigation_id = ? AND o.collector_run_id = ?
              AND a.workspace_id = ? AND a.meta_ad_id = ?
            """,
            Integer.class,
            investigationId,
            collectorRunId,
            workspaceId,
            metaAdId);
    return count != null && count > 0;
  }

  /** Finaliza tecnicamente a execução preservando o gate comercial calculado. */
  public MoisMetaAdDtos.InvestigationResponse complete(
      long id, MoisMetaAdDtos.CompleteInvestigationRequest request) {
    MoisMetaAdDtos.InvestigationResponse investigation = required(id);
    if (request.success()) {
      persistGate(id, calculateGate(investigation.workspaceId(), id));
    }
    jdbcTemplate.update(
        "UPDATE mois_meta_ad_investigation SET status = ?, error_message = ?, finished_at = ?, next_run_at = ?, updated_at = ? WHERE id = ?",
        request.success() ? "COMPLETED" : "FAILED",
        request.errorMessage(),
        Timestamp.from(Instant.now()),
        request.success() ? Timestamp.from(Instant.now().plus(Duration.ofDays(1))) : null,
        Timestamp.from(Instant.now()),
        id);
    return required(id);
  }

  /** Consolida um anúncio sem apagar o histórico entre investigações sucessivas. */
  private long upsertAsset(
      String workspaceId, MoisMetaAdDtos.MetaAdObservation item, Instant observedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO mois_meta_ad_asset
          (workspace_id, meta_ad_id, advertiser_id, advertiser_name, ad_status,
           publisher_platforms_json, format_types_json, ad_texts_json, media_json,
           destination_url, snapshot_url, first_observed_at, last_observed_at, observation_count,
           page_active, commercial_signal, raw_payload_json, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          advertiser_id = VALUES(advertiser_id), advertiser_name = VALUES(advertiser_name),
          ad_status = VALUES(ad_status),
          publisher_platforms_json = VALUES(publisher_platforms_json),
          format_types_json = VALUES(format_types_json),
          ad_texts_json = VALUES(ad_texts_json), media_json = VALUES(media_json),
          destination_url = COALESCE(VALUES(destination_url), destination_url),
          snapshot_url = COALESCE(VALUES(snapshot_url), snapshot_url),
          last_observed_at = GREATEST(last_observed_at, VALUES(last_observed_at)),
          observation_count = observation_count + 1, page_active = VALUES(page_active),
          commercial_signal = VALUES(commercial_signal), raw_payload_json = VALUES(raw_payload_json),
          updated_at = VALUES(updated_at)
        """,
        workspaceId,
        item.metaAdId(),
        item.advertiserId(),
        item.advertiserName(),
        item.status(),
        json(item.publisherPlatforms()),
        json(item.formatTypes()),
        json(item.texts()),
        json(item.mediaUrls()),
        item.destinationUrl(),
        item.snapshotUrl(),
        Timestamp.from(observedAt),
        Timestamp.from(observedAt),
        item.pageActive(),
        item.commercialSignal(),
        item.rawPayload(),
        Timestamp.from(Instant.now()),
        Timestamp.from(Instant.now()));
    return jdbcTemplate.queryForObject(
        "SELECT id FROM mois_meta_ad_asset WHERE workspace_id = ? AND meta_ad_id = ?",
        Long.class,
        workspaceId,
        item.metaAdId());
  }

  /** Calcula longevidade, validação plausível e aderência ao Hub como diagnósticos separados. */
  private GateResult calculateGate(String workspaceId, long investigationId) {
    Map<String, Object> metrics =
        jdbcTemplate.queryForMap(
            """
            SELECT COUNT(DISTINCT a.id) ads,
                   COUNT(DISTINCT a.advertiser_id) advertisers,
                   MAX(DATEDIFF(a.last_observed_at, a.first_observed_at)) max_longevity,
                   MAX(a.observation_count) max_observations,
                   SUM(CASE WHEN a.page_active = 1 THEN 1 ELSE 0 END) active_pages,
                   SUM(CASE WHEN a.commercial_signal = 1 THEN 1 ELSE 0 END) commercial_signals
            FROM mois_meta_ad_observation o
            JOIN mois_meta_ad_asset a ON a.id = o.asset_id
            WHERE o.investigation_id = ? AND a.workspace_id = ?
            """,
            investigationId,
            workspaceId);
    int ads = number(metrics.get("ads"));
    int longevity = number(metrics.get("max_longevity"));
    int observations = number(metrics.get("max_observations"));
    int activePages = number(metrics.get("active_pages"));
    int commercialSignals = number(metrics.get("commercial_signals"));
    boolean longRunning = longevity >= MIN_LONGEVITY_DAYS && observations >= MIN_OBSERVATIONS;
    boolean productValidated =
        longRunning && ads >= MIN_VARIATIONS && activePages > 0 && commercialSignals > 0;
    boolean hubOpportunity = productValidated;
    List<String> evidence = new ArrayList<>();
    List<String> gaps = new ArrayList<>();
    evidence.add(ads + " anúncios/variações observados por payload real");
    if (longRunning)
      evidence.add("Longevidade confirmada por observações sucessivas: " + longevity + " dias");
    else gaps.add("Confirmar o mesmo anúncio em pelo menos duas coletas separadas por 30 dias");
    if (ads < MIN_VARIATIONS) gaps.add("Encontrar ao menos três variações do produto/anunciante");
    if (activePages == 0) gaps.add("Confirmar página de destino ativa");
    if (commercialSignals == 0)
      gaps.add("Confirmar sinal comercial externo, como preço ou checkout");
    String decision = productValidated ? "MODELAR" : (ads == 0 ? "DESCARTAR" : "INVESTIGAR");
    MoisMetaAdDtos.EthicalModelingCard card = buildEthicalCard(productValidated);
    return new GateResult(decision, evidence, gaps, card);
  }

  /** Persiste decisão, evidências, lacunas e ficha ética para relatório da tela. */
  private void persistGate(long investigationId, GateResult gate) {
    jdbcTemplate.update(
        "UPDATE mois_meta_ad_investigation SET gate_decision = ?, evidence_json = ?, gaps_json = ?, ethical_modeling_json = ?, updated_at = ? WHERE id = ?",
        gate.decision(),
        json(gate.evidence()),
        json(gate.gaps()),
        json(gate.card()),
        Timestamp.from(Instant.now()),
        investigationId);
  }

  /** Gera apenas estrutura de modelagem, nunca conteúdo identificável do concorrente. */
  private MoisMetaAdDtos.EthicalModelingCard buildEthicalCard(boolean qualified) {
    if (!qualified) return MoisMetaAdDtos.EthicalModelingCard.empty();
    return new MoisMetaAdDtos.EthicalModelingCard(
        "Extrair a dor recorrente das evidências, sem copiar a redação",
        "Descrever o público pelo problema e contexto",
        "Modelar o princípio do mecanismo com diferenciação própria",
        "Mapear entregáveis, prova, redução de risco e CTA",
        List.of("dor", "demonstração", "objeção", "resultado plausível"),
        List.of("sequência narrativa", "tipo de prova", "formato de oferta"),
        List.of("marca", "texto", "personagem", "criativo", "mídia"));
  }

  /** Recupera uma investigação ou interrompe a operação com contexto. */
  private MoisMetaAdDtos.InvestigationResponse required(long id) {
    return get(id)
        .orElseThrow(() -> new IllegalArgumentException("Investigação Meta não encontrada: " + id));
  }

  /** Normaliza o país para o padrão usado pela API da Meta. */
  private String normalizedCountry(String country) {
    return country == null || country.isBlank() ? "BR" : country.trim().toUpperCase(Locale.ROOT);
  }

  /** Expõe o modo real de coleta, a busca oficial e a próxima observação esperada. */
  MoisMetaAdDtos.CollectionState collectionState(
      String country,
      String publisherPlatform,
      String searchTerms,
      Instant firstObservedAt,
      Instant createdAt,
      Instant nextRunAt) {
    boolean officialApi = supportsOfficialCommercialApi(country);
    Instant nextObservationAt =
        officialApi
            ? (nextRunAt == null ? createdAt : nextRunAt)
            : firstObservedAt == null
                ? createdAt
                : firstObservedAt.plus(Duration.ofDays(MIN_LONGEVITY_DAYS));
    return new MoisMetaAdDtos.CollectionState(
        officialApi ? "OFFICIAL_API" : "SUPERVISED",
        officialApi ? OFFICIAL_API_COLLECTION_REASON : SUPERVISED_COLLECTION_REASON,
        buildPublicSearchUrl(country, publisherPlatform, searchTerms),
        nextObservationAt);
  }

  /** Informa se anúncios comerciais gerais podem ser consultados oficialmente no território. */
  boolean supportsOfficialCommercialApi(String country) {
    return OFFICIAL_COMMERCIAL_API_COUNTRIES.contains(normalizedCountry(country));
  }

  /** Normaliza a plataforma pedida para uma tecnologia aceita pela Biblioteca Meta. */
  private String normalizedPublisherPlatform(String publisherPlatform) {
    String normalized =
        publisherPlatform == null || publisherPlatform.isBlank()
            ? "INSTAGRAM"
            : publisherPlatform.trim().toUpperCase(Locale.ROOT);
    if (!Set.of("FACEBOOK", "INSTAGRAM", "AUDIENCE_NETWORK", "MESSENGER", "WHATSAPP")
        .contains(normalized)) {
      throw new IllegalArgumentException("Plataforma da Biblioteca Meta não suportada");
    }
    return normalized;
  }

  /** Preserva somente plataformas declaradas e usa a investigação como fallback compatível. */
  private List<String> normalizePublisherPlatforms(
      List<String> publisherPlatforms, String investigationPlatform) {
    if (publisherPlatforms == null || publisherPlatforms.isEmpty()) {
      return List.of(normalizedPublisherPlatform(investigationPlatform));
    }
    return publisherPlatforms.stream().map(this::normalizedPublisherPlatform).distinct().toList();
  }

  /** Monta um atalho para a busca pública sem autenticar nem raspar a interface da Meta. */
  private String buildPublicSearchUrl(
      String country, String publisherPlatform, String searchTerms) {
    return "https://www.facebook.com/ads/library/?active_status=active&ad_type=all&country="
        + URLEncoder.encode(normalizedCountry(country), StandardCharsets.UTF_8)
        + "&media_type=all&q="
        + URLEncoder.encode(searchTerms == null ? "" : searchTerms.trim(), StandardCharsets.UTF_8)
        + "&search_type=keyword_unordered";
  }

  /** Converte texto opcional vazio em nulo para preservar a ausência de evidência. */
  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  /** Converte valores agregados SQL em inteiros seguros. */
  private int number(Object value) {
    return value instanceof Number number ? number.intValue() : 0;
  }

  /** Serializa contratos auxiliares para colunas JSON auditáveis. */
  private String json(Object value) {
    try {
      return objectMapper.writeValueAsString(value == null ? List.of() : value);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao serializar auditoria da investigação Meta", ex);
      throw new IllegalStateException("Falha ao serializar investigação Meta", ex);
    }
  }

  /** Desserializa listas textuais persistidas. */
  private List<String> stringList(String value) {
    try {
      return value == null ? List.of() : objectMapper.readValue(value, new TypeReference<>() {});
    } catch (JsonProcessingException ex) {
      log.error("Falha ao ler lista auditável da investigação Meta", ex);
      throw new IllegalStateException("Falha ao ler investigação Meta", ex);
    }
  }

  /** Desserializa a ficha de modelagem ética persistida. */
  private MoisMetaAdDtos.EthicalModelingCard modelingCard(String value) {
    try {
      return value == null
          ? MoisMetaAdDtos.EthicalModelingCard.empty()
          : objectMapper.readValue(value, MoisMetaAdDtos.EthicalModelingCard.class);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao ler ficha ética da investigação Meta", ex);
      throw new IllegalStateException("Falha ao ler ficha ética Meta", ex);
    }
  }

  /** Desserializa o briefing criativo sem fabricar conteúdo quando ele ainda não existe. */
  private MoisMetaAdDtos.CreativeIntelligenceBrief creativeBrief(String value) {
    try {
      return value == null
          ? MoisMetaAdDtos.CreativeIntelligenceBrief.unavailable()
          : objectMapper.readValue(value, MoisMetaAdDtos.CreativeIntelligenceBrief.class);
    } catch (JsonProcessingException ex) {
      log.error("Falha ao ler briefing da inteligência criativa Meta", ex);
      throw new IllegalStateException("Falha ao ler briefing da inteligência criativa Meta", ex);
    }
  }

  /** Seleciona um padrão comprovado ou mantém uma formulação neutra. */
  private String firstOrFallback(List<String> values, String fallback) {
    return values == null || values.isEmpty() ? fallback : values.getFirst();
  }

  /** Agrupa a decisão calculada e sua justificativa auditável. */
  private record GateResult(
      String decision,
      List<String> evidence,
      List<String> gaps,
      MoisMetaAdDtos.EthicalModelingCard card) {}
}
