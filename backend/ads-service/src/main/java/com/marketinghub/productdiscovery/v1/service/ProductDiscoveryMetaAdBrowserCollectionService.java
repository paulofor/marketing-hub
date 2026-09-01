package com.marketinghub.productdiscovery.v1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Persiste a coleta pública do Chromium de Argos e preserva o fallback humano auditável. */
@Service
public class ProductDiscoveryMetaAdBrowserCollectionService {
  private static final Logger log =
      LoggerFactory.getLogger(ProductDiscoveryMetaAdBrowserCollectionService.class);
  private static final String WORKSPACE_ID = "workspace-001";
  private static final Duration MAX_BROWSER_DURATION = Duration.ofMinutes(5);
  private static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofMinutes(5);

  private final ProductDiscoveryCycleRepository cycleRepository;
  private final ProductDiscoveryMetaAdSessionLinkService sessionLinkService;
  private final ProductDiscoveryMetaAdEvidenceService evidenceService;
  private final MoisMetaAdInvestigationService investigationService;
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /** Configura o serviço com as fontes de verdade do ciclo, da investigação e da auditoria. */
  public ProductDiscoveryMetaAdBrowserCollectionService(
      ProductDiscoveryCycleRepository cycleRepository,
      ProductDiscoveryMetaAdSessionLinkService sessionLinkService,
      ProductDiscoveryMetaAdEvidenceService evidenceService,
      MoisMetaAdInvestigationService investigationService,
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper) {
    this.cycleRepository = cycleRepository;
    this.sessionLinkService = sessionLinkService;
    this.evidenceService = evidenceService;
    this.investigationService = investigationService;
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  /** Valida, registra o payload bruto e incorpora cards públicos à investigação exata. */
  @Transactional
  public ProductDiscoveryMetaAdEvidenceListResponse record(
      Long cycleId, ProductDiscoveryMetaAdBrowserCollectionRequest request) {
    ProductDiscoveryCycle cycle = requiredActiveCycle(cycleId, request.executionLeaseId());
    MoisMetaAdDtos.InvestigationResponse investigation = requiredInvestigation(cycle, request);
    validateCollection(cycle, investigation, request);
    String rawPayload = serialize(request, cycleId);
    String previousPayload = previousRawPayload(cycleId, request);
    if (previousPayload != null) {
      if (!sameJson(previousPayload, rawPayload, cycleId)) {
        throw conflict("O collectorRunId já foi usado com outro payload");
      }
      return evidenceService.searchInvestigation(cycleId, investigation, 50);
    }

    log.info(
        "Product Discovery recebeu coleta Meta pública bruta cycleId={} attemptNumber={} investigationId={} collectorRunId={} payload={}",
        cycleId,
        request.attemptNumber(),
        investigation.id(),
        request.collectorRunId(),
        rawPayload);
    insertBrowserRun(cycleId, request, rawPayload);
    if ("OBSERVED".equals(request.outcome())) {
      investigationService.ingest(
          investigation.id(),
          new MoisMetaAdDtos.ObservationBatchRequest(
              request.collectorRunId(),
              request.observations().stream().map(this::toMetaObservation).toList(),
              request.finishedAt()));
    }
    return evidenceService.searchInvestigation(cycleId, investigation, 50);
  }

  /** Exige que o callback ainda pertença ao lease ativo e ao ciclo B2C para Instagram. */
  private ProductDiscoveryCycle requiredActiveCycle(Long cycleId, String executionLeaseId) {
    ProductDiscoveryCycle cycle =
        cycleRepository
            .findByIdForUpdate(cycleId)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Ciclo de descoberta não encontrado"));
    if (cycle.getStatus() != ProductDiscoveryCycleStatus.RESEARCHING
        || !StringUtils.hasText(executionLeaseId)
        || !executionLeaseId.equals(cycle.getExecutionLeaseId())) {
      throw conflict("Lease da execução de descoberta expirou ou foi substituído");
    }
    String channel =
        StringUtils.hasText(cycle.getAcquisitionChannel())
            ? cycle.getAcquisitionChannel().trim().toLowerCase(Locale.ROOT)
            : "";
    if (cycle.getMarketType() != ProductDiscoveryMarketType.B2C || !channel.contains("instagram")) {
      throw conflict("O navegador Meta de Argos exige um ciclo B2C orientado ao Instagram");
    }
    return cycle;
  }

  /** Recupera somente a investigação previamente congelada para a tentativa informada. */
  private MoisMetaAdDtos.InvestigationResponse requiredInvestigation(
      ProductDiscoveryCycle cycle, ProductDiscoveryMetaAdBrowserCollectionRequest request) {
    return sessionLinkService
        .linkedAttemptInvestigation(cycle, request.attemptNumber())
        .filter(item -> item.id() == request.investigationId())
        .orElseThrow(
            () -> conflict("A coleta pública não pertence à investigação vinculada à tentativa"));
  }

  /** Confirma fonte, filtros, tempos e coerência entre desfecho e cards recebidos. */
  private void validateCollection(
      ProductDiscoveryCycle cycle,
      MoisMetaAdDtos.InvestigationResponse investigation,
      ProductDiscoveryMetaAdBrowserCollectionRequest request) {
    if (!WORKSPACE_ID.equals(investigation.workspaceId())
        || !"INSTAGRAM".equalsIgnoreCase(investigation.publisherPlatform())
        || !cycle.getCountry().equalsIgnoreCase(investigation.countryCode())) {
      throw conflict("A investigação Meta diverge do território ou da plataforma do ciclo");
    }
    if (!request.searchUrl().equals(investigation.collection().searchUrl())) {
      throw conflict("A coleta pública não usou a URL oficial preparada pelo backend");
    }
    Duration duration = Duration.between(request.startedAt(), request.finishedAt());
    if (duration.isNegative() || duration.compareTo(MAX_BROWSER_DURATION) > 0) {
      throw conflict("A duração da coleta pública é inválida");
    }
    if (request.startedAt().isAfter(Instant.now().plus(CLOCK_SKEW_TOLERANCE))) {
      throw conflict("O início da coleta pública está além da tolerância de relógio");
    }
    boolean hasObservations = !request.observations().isEmpty();
    if ("OBSERVED".equals(request.outcome())
        && (!hasObservations || !request.platformFilterConfirmed())) {
      throw conflict("OBSERVED exige cards e confirmação do filtro Instagram");
    }
    if ("EMPTY".equals(request.outcome())
        && (hasObservations || !request.platformFilterConfirmed())) {
      throw conflict("EMPTY exige resultado vazio explícito e filtros confirmados");
    }
    if ("FALLBACK_REQUIRED".equals(request.outcome())
        && (hasObservations || !StringUtils.hasText(request.errorMessage()))) {
      throw conflict("FALLBACK_REQUIRED exige causa e não aceita cards ambíguos");
    }
    if (request.observations().stream().anyMatch(item -> !item.active())) {
      throw conflict("A coleta limitada aceita somente anúncios ativos");
    }
  }

  /** Transforma o card validado no contrato canônico do radar sem descartar o payload bruto. */
  private MoisMetaAdDtos.MetaAdObservation toMetaObservation(
      ProductDiscoveryMetaAdBrowserCollectionRequest.Observation observation) {
    return new MoisMetaAdDtos.MetaAdObservation(
        observation.metaAdId().trim(),
        null,
        observation.advertiserName().trim(),
        "ACTIVE",
        observation.publisherPlatforms(),
        observation.formatTypes(),
        observation.texts(),
        List.of(),
        blankToNull(observation.destinationUrl()),
        observation.snapshotUrl().trim(),
        observation.pageActive(),
        observation.commercialSignal(),
        normalizedObservationPayload(observation));
  }

  /** Mantém os campos legados de leitura e anexa a observação original feita no browser. */
  private String normalizedObservationPayload(
      ProductDiscoveryMetaAdBrowserCollectionRequest.Observation observation) {
    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("adReference", observation.metaAdId().trim());
    payload.put("advertiserName", observation.advertiserName().trim());
    payload.put("adLibraryUrl", observation.snapshotUrl().trim());
    payload.put("adText", String.join("\n", observation.texts()).trim());
    payload.set("publisherPlatforms", objectMapper.valueToTree(observation.publisherPlatforms()));
    if (!observation.formatTypes().isEmpty()) {
      payload.put("formatType", observation.formatTypes().getFirst());
    }
    if (StringUtils.hasText(observation.destinationUrl())) {
      payload.put("destinationUrl", observation.destinationUrl().trim());
    }
    payload.put("pageActive", observation.pageActive());
    payload.put("commercialSignal", observation.commercialSignal());
    payload.set("browserRawPayload", observation.rawPayload());
    return payload.toString();
  }

  /** Persiste a execução técnica antes de normalizar e anexar seus cards comerciais. */
  private void insertBrowserRun(
      Long cycleId, ProductDiscoveryMetaAdBrowserCollectionRequest request, String rawPayload) {
    jdbcTemplate.update(
        """
        INSERT INTO product_discovery_meta_browser_run
          (cycle_id, attempt_number, investigation_id, execution_lease_id, collector_run_id, search_url,
           outcome, http_status, platform_filter_confirmed, page_title, result_count,
           error_message, raw_payload_json, started_at, finished_at, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        cycleId,
        request.attemptNumber(),
        request.investigationId(),
        request.executionLeaseId(),
        request.collectorRunId(),
        request.searchUrl(),
        request.outcome(),
        request.httpStatus(),
        request.platformFilterConfirmed(),
        blankToNull(request.pageTitle()),
        request.observations().size(),
        blankToNull(request.errorMessage()),
        rawPayload,
        Timestamp.from(request.startedAt()),
        Timestamp.from(request.finishedAt()),
        Timestamp.from(Instant.now()));
  }

  /** Lê um retry já auditado para impedir que o mesmo lote infle a longevidade. */
  private String previousRawPayload(
      Long cycleId, ProductDiscoveryMetaAdBrowserCollectionRequest request) {
    List<String> payloads =
        jdbcTemplate.query(
            """
            SELECT raw_payload_json
            FROM product_discovery_meta_browser_run
            WHERE cycle_id = ? AND execution_lease_id = ? AND collector_run_id = ?
            """,
            (rs, rowNum) -> rs.getString("raw_payload_json"),
            cycleId,
            request.executionLeaseId(),
            request.collectorRunId());
    return payloads.isEmpty() ? null : payloads.getFirst();
  }

  /** Compara JSON estrutural para aceitar retry byte-a-byte equivalente. */
  private boolean sameJson(String previous, String current, Long cycleId) {
    try {
      JsonNode previousJson = objectMapper.readTree(previous);
      JsonNode currentJson = objectMapper.readTree(current);
      return previousJson != null && previousJson.equals(currentJson);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao comparar retry da coleta Meta pública cycleId={} operação=sameJson",
          cycleId,
          ex);
      throw new IllegalStateException("Auditoria da coleta pública possui JSON inválido", ex);
    }
  }

  /** Serializa o payload recebido sem ocultar os fatos que serão normalizados. */
  private String serialize(ProductDiscoveryMetaAdBrowserCollectionRequest request, Long cycleId) {
    try {
      return objectMapper.writeValueAsString(request);
    } catch (JsonProcessingException ex) {
      log.error(
          "Falha ao serializar coleta Meta pública cycleId={} operação=serialize", cycleId, ex);
      throw new ResponseStatusException(
          HttpStatus.UNPROCESSABLE_ENTITY, "Payload da coleta pública é inválido", ex);
    }
  }

  /** Normaliza texto opcional sem fabricar um valor ausente. */
  private String blankToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  /** Produz um conflito funcional legível para o executor. */
  private ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }
}
