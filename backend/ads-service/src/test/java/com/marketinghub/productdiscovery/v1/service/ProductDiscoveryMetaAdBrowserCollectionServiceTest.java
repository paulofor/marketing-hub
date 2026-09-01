package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycle;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryCycleStatus;
import com.marketinghub.productdiscovery.v1.ProductDiscoveryMarketType;
import com.marketinghub.repository.jpa.productdiscovery.ProductDiscoveryCycleRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Comprova auditoria, idempotência e fallback da coleta pública executada por Argos. */
class ProductDiscoveryMetaAdBrowserCollectionServiceTest {
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final ProductDiscoveryCycleRepository cycleRepository =
      Mockito.mock(ProductDiscoveryCycleRepository.class);
  private final ProductDiscoveryMetaAdSessionLinkService sessionLinkService =
      Mockito.mock(ProductDiscoveryMetaAdSessionLinkService.class);
  private final ProductDiscoveryMetaAdEvidenceService evidenceService =
      Mockito.mock(ProductDiscoveryMetaAdEvidenceService.class);
  private final MoisMetaAdInvestigationService investigationService =
      Mockito.mock(MoisMetaAdInvestigationService.class);
  private JdbcTemplate jdbcTemplate;
  private ProductDiscoveryMetaAdBrowserCollectionService service;
  private ProductDiscoveryCycle cycle;
  private MoisMetaAdDtos.InvestigationResponse investigation;

  /** Prepara o ciclo ativo, a investigação congelada e a tabela de auditoria efêmera. */
  @BeforeEach
  void setUp() {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(
            "jdbc:h2:mem:argos_meta_browser_"
                + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute(
        """
        CREATE TABLE product_discovery_meta_browser_run (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          cycle_id BIGINT NOT NULL,
          attempt_number INT NOT NULL,
          investigation_id BIGINT NOT NULL,
          execution_lease_id VARCHAR(36) NOT NULL,
          collector_run_id VARCHAR(80) NOT NULL,
          search_url VARCHAR(2048) NOT NULL,
          outcome VARCHAR(40) NOT NULL,
          http_status INT,
          platform_filter_confirmed TINYINT NOT NULL,
          page_title VARCHAR(255),
          result_count INT NOT NULL,
          error_message TEXT,
          raw_payload_json LONGTEXT NOT NULL,
          started_at DATETIME NOT NULL,
          finished_at DATETIME NOT NULL,
          created_at DATETIME NOT NULL,
          UNIQUE (cycle_id, execution_lease_id, collector_run_id)
        )
        """);
    cycle = activeCycle();
    investigation = investigation();
    when(cycleRepository.findByIdForUpdate(144L)).thenReturn(Optional.of(cycle));
    when(sessionLinkService.linkedAttemptInvestigation(cycle, 1))
        .thenReturn(Optional.of(investigation));
    ProductDiscoveryMetaAdEvidenceListResponse response = evidenceResponse("OBSERVED");
    when(evidenceService.searchInvestigation(eq(144L), eq(investigation), eq(50)))
        .thenReturn(response);
    service =
        new ProductDiscoveryMetaAdBrowserCollectionService(
            cycleRepository,
            sessionLinkService,
            evidenceService,
            investigationService,
            jdbcTemplate,
            objectMapper);
  }

  /** Deve persistir o card bruto uma vez e aceitar retry equivalente sem inflar observações. */
  @Test
  void recordsObservedBatchIdempotently() {
    ProductDiscoveryMetaAdBrowserCollectionRequest request = observedRequest();

    ProductDiscoveryMetaAdEvidenceListResponse first = service.record(144L, request);
    ProductDiscoveryMetaAdEvidenceListResponse retry = service.record(144L, request);

    assertThat(first.sourceStatus()).isEqualTo("OBSERVED");
    assertThat(retry.sourceStatus()).isEqualTo("OBSERVED");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_discovery_meta_browser_run", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT raw_payload_json FROM product_discovery_meta_browser_run", String.class))
        .contains("META_AD_LIBRARY_PUBLIC_BROWSER")
        .contains("meta-144");
    ArgumentCaptor<MoisMetaAdDtos.ObservationBatchRequest> batch =
        ArgumentCaptor.forClass(MoisMetaAdDtos.ObservationBatchRequest.class);
    verify(investigationService, times(1)).ingest(eq(91L), batch.capture());
    assertThat(batch.getValue().observations()).hasSize(1);
    assertThat(batch.getValue().observations().getFirst().publisherPlatforms())
        .containsExactly("INSTAGRAM");
  }

  /** Deve registrar bloqueio como fallback humano sem criar anúncio ou ausência fictícia. */
  @Test
  void recordsFallbackWithoutIngestingAds() {
    ProductDiscoveryMetaAdBrowserCollectionRequest fallback =
        new ProductDiscoveryMetaAdBrowserCollectionRequest(
            "lease-144",
            1,
            91L,
            "argos-browser-144-fallback",
            investigation.collection().searchUrl(),
            "FALLBACK_REQUIRED",
            403,
            false,
            "Biblioteca de Anúncios",
            "A Biblioteca exigiu verificação humana.",
            Instant.parse("2026-08-30T12:00:00Z"),
            Instant.parse("2026-08-30T12:00:02Z"),
            List.of());

    service.record(144L, fallback);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT outcome FROM product_discovery_meta_browser_run", String.class))
        .isEqualTo("FALLBACK_REQUIRED");
    verify(investigationService, never()).ingest(any(Long.class), any());
  }

  /** Deve tolerar pequeno desvio entre hosts e rejeitar tempos operacionalmente impossíveis. */
  @Test
  void validatesDurationAndClockSkewWithoutRequiringIdenticalHostClocks() {
    Instant now = Instant.now();
    ProductDiscoveryMetaAdBrowserCollectionRequest acceptableSkew =
        fallbackRequest("argos-browser-144-skew", now.plusSeconds(240), now.plusSeconds(242));

    service.record(144L, acceptableSkew);

    ProductDiscoveryMetaAdBrowserCollectionRequest excessiveSkew =
        fallbackRequest("argos-browser-144-future", now.plusSeconds(360), now.plusSeconds(362));
    ProductDiscoveryMetaAdBrowserCollectionRequest excessiveDuration =
        fallbackRequest("argos-browser-144-duration", now.minusSeconds(361), now);
    assertThatThrownBy(() -> service.record(144L, excessiveSkew))
        .hasMessageContaining("tolerância de relógio");
    assertThatThrownBy(() -> service.record(144L, excessiveDuration))
        .hasMessageContaining("duração da coleta pública");
  }

  /** Monta o ciclo B2C Instagram com lease e investigação imutáveis. */
  private ProductDiscoveryCycle activeCycle() {
    ProductDiscoveryCycle item = new ProductDiscoveryCycle();
    item.setId(144L);
    item.setCountry("BR");
    item.setAcquisitionChannel("Instagram Reels");
    item.setMarketType(ProductDiscoveryMarketType.B2C);
    item.setStatus(ProductDiscoveryCycleStatus.RESEARCHING);
    item.setExecutionLeaseId("lease-144");
    item.setMetaAdInvestigationId(91L);
    return item;
  }

  /** Monta a investigação oficial preparada pelo backend para a consulta pública. */
  private MoisMetaAdDtos.InvestigationResponse investigation() {
    Instant now = Instant.parse("2026-08-30T12:00:00Z");
    return new MoisMetaAdDtos.InvestigationResponse(
        91L,
        "workspace-001",
        "guarda roupa cápsula climatério",
        "BR",
        "INSTAGRAM",
        "ACTIVE_SUPERVISED",
        new MoisMetaAdDtos.CollectionState(
            "SUPERVISED",
            "Consulta pública limitada.",
            "https://www.facebook.com/ads/library/?country=BR&q=guarda+roupa",
            now),
        "INVESTIGAR",
        List.of(),
        List.of(),
        MoisMetaAdDtos.EthicalModelingCard.empty(),
        MoisMetaAdDtos.CreativeIntelligenceBrief.unavailable(),
        0,
        now,
        now);
  }

  /** Monta o lote visível usado pelo caminho feliz e pelo retry. */
  private ProductDiscoveryMetaAdBrowserCollectionRequest observedRequest() {
    return new ProductDiscoveryMetaAdBrowserCollectionRequest(
        "lease-144",
        1,
        91L,
        "argos-browser-144-lease-144",
        investigation.collection().searchUrl(),
        "OBSERVED",
        403,
        true,
        "Biblioteca de Anúncios",
        null,
        Instant.parse("2026-08-30T12:00:00Z"),
        Instant.parse("2026-08-30T12:00:02Z"),
        List.of(
            new ProductDiscoveryMetaAdBrowserCollectionRequest.Observation(
                "meta-144",
                "Estilo Maduro",
                true,
                List.of("INSTAGRAM"),
                List.of("VIDEO"),
                List.of("Guarda roupa cápsula para o climatério por R$ 49."),
                "https://estilo.example/oferta",
                "https://www.facebook.com/ads/library/?id=meta-144",
                true,
                true,
                objectMapper.valueToTree(
                    java.util.Map.of("source", "META_AD_LIBRARY_PUBLIC_BROWSER")))));
  }

  /** Monta um fallback controlado com os tempos necessários ao contrato sob teste. */
  private ProductDiscoveryMetaAdBrowserCollectionRequest fallbackRequest(
      String collectorRunId, Instant startedAt, Instant finishedAt) {
    return new ProductDiscoveryMetaAdBrowserCollectionRequest(
        "lease-144",
        1,
        91L,
        collectorRunId,
        investigation.collection().searchUrl(),
        "FALLBACK_REQUIRED",
        403,
        false,
        "Biblioteca de Anúncios",
        "A Biblioteca exigiu verificação humana.",
        startedAt,
        finishedAt,
        List.of());
  }

  /** Monta o retorno já consolidado entregue ao executor. */
  private ProductDiscoveryMetaAdEvidenceListResponse evidenceResponse(String sourceStatus) {
    return new ProductDiscoveryMetaAdEvidenceListResponse(
        144L,
        "guarda roupa cápsula climatério",
        "BR",
        "INSTAGRAM",
        sourceStatus,
        "PUBLIC_BROWSER",
        91L,
        investigation.collection().searchUrl(),
        1,
        1,
        1,
        Instant.parse("2026-08-30T12:00:02Z"),
        "Presença publicitária não comprova vendas.",
        List.of());
  }
}
