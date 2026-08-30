package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Valida relevância, plataforma e atualidade da cobertura Meta entregue a Argos. */
class ProductDiscoveryMetaAdEvidenceServiceTest {

  private JdbcTemplate jdbcTemplate;
  private MoisMetaAdInvestigationService investigationService;
  private ProductDiscoveryMetaAdSessionLinkService sessionLinkService;
  private ProductDiscoveryMetaAdEvidenceService service;

  /** Prepara um banco efêmero com o contrato mínimo do radar Meta. */
  @BeforeEach
  void setUp() {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(
            "jdbc:h2:mem:product_discovery_meta_"
                + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    jdbcTemplate = new JdbcTemplate(dataSource);
    investigationService = Mockito.mock(MoisMetaAdInvestigationService.class);
    sessionLinkService = Mockito.mock(ProductDiscoveryMetaAdSessionLinkService.class);
    service =
        new ProductDiscoveryMetaAdEvidenceService(
            jdbcTemplate, investigationService, sessionLinkService, new ObjectMapper());
    when(sessionLinkService.linkedInvestigation(Mockito.anyLong())).thenReturn(Optional.empty());
    when(sessionLinkService.bindActiveInvestigation(
            Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString()))
        .thenAnswer(
            invocation -> {
              long investigationId = invocation.getArgument(1);
              return investigation(investigationId, 0);
            });
    createSchema();
  }

  /** Deve aceitar somente anúncio atual e explicitamente distribuído no Instagram. */
  @Test
  void usesOnlyFreshInstagramEvidenceForTheActiveMetric() {
    Instant now = Instant.now();
    insertInvestigation(7L, "BR");
    insertAsset(
        1L,
        "ad-instagram-current",
        "Treino Entrevista",
        "Treino entrevista emprego para jovens",
        "[\"INSTAGRAM\"]",
        true,
        now.minusSeconds(86_400L));
    insertAsset(
        2L,
        "ad-facebook-current",
        "Treino Entrevista Facebook",
        "Treino entrevista emprego para jovens",
        "[\"FACEBOOK\"]",
        true,
        now.minusSeconds(86_400L));
    insertAsset(
        3L,
        "ad-instagram-stale",
        "Entrevista Antiga",
        "Treino entrevista emprego antigo",
        "[\"INSTAGRAM\"]",
        true,
        now.minusSeconds(40L * 86_400L));
    insertObservation(1L, 7L, 1L, now.minusSeconds(86_400L));
    insertObservation(2L, 7L, 2L, now.minusSeconds(86_400L));
    insertObservation(3L, 7L, 3L, now.minusSeconds(40L * 86_400L));
    when(investigationService.ensureForProductDiscovery(
            "workspace-001", "treino entrevista emprego", "BR", "INSTAGRAM"))
        .thenReturn(investigation(7L, 3));
    when(sessionLinkService.bindActiveInvestigation(81L, 7L, "lease-81"))
        .thenReturn(investigation(7L, 3));

    ProductDiscoveryMetaAdEvidenceListResponse response =
        service.requestAndSearch(
            81L,
            new ProductDiscoveryMetaAdEvidenceRequest(
                "lease-81", "treino entrevista emprego", "BR", "INSTAGRAM", 25));

    assertThat(response.sourceStatus()).isEqualTo("OBSERVED");
    assertThat(response.adsObserved()).isEqualTo(2);
    assertThat(response.activeAds()).isEqualTo(1);
    assertThat(response.advertisersObserved()).isEqualTo(2);
    assertThat(response.items())
        .extracting(ProductDiscoveryMetaAdEvidenceResponse::metaAdId)
        .containsExactlyInAnyOrder("ad-instagram-current", "ad-instagram-stale");
    assertThat(response.items().getFirst().publisherPlatforms()).containsExactly("INSTAGRAM");
    assertThat(response.items().getFirst().adTexts())
        .containsExactly("Treino entrevista emprego para jovens");
  }

  /** Deve marcar a cobertura como desatualizada quando só existir observação antiga. */
  @Test
  void marksCoverageAsStaleWhenOnlyOldEvidenceExists() {
    Instant oldObservation = Instant.now().minusSeconds(40L * 86_400L);
    insertInvestigation(8L, "BR");
    insertAsset(
        4L,
        "ad-salario-stale",
        "Negociação Salarial",
        "Ensaio negociação salarial carreira",
        "[\"INSTAGRAM\"]",
        true,
        oldObservation);
    insertObservation(4L, 8L, 4L, oldObservation);
    when(investigationService.ensureForProductDiscovery(
            "workspace-001", "negociacao salarial carreira", "BR", "INSTAGRAM"))
        .thenReturn(investigation(8L, 1));
    when(sessionLinkService.bindActiveInvestigation(82L, 8L, "lease-82"))
        .thenReturn(investigation(8L, 1));

    ProductDiscoveryMetaAdEvidenceListResponse response =
        service.requestAndSearch(
            82L,
            new ProductDiscoveryMetaAdEvidenceRequest(
                "lease-82", "negociacao salarial carreira", "BR", "INSTAGRAM", 25));

    assertThat(response.sourceStatus()).isEqualTo("STALE");
    assertThat(response.activeAds()).isZero();
  }

  /** Deve impedir consulta ampla que possa anexar anúncios irrelevantes ao dossiê. */
  @Test
  void rejectsQueryWithoutTwoSpecificTerms() {
    assertThatThrownBy(
            () ->
                service.requestAndSearch(
                    83L,
                    new ProductDiscoveryMetaAdEvidenceRequest(
                        "lease-83", "produto Instagram", "BR", "INSTAGRAM", 25)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("dois termos específicos");
    verifyNoInteractions(investigationService);
  }

  /** Deve rejeitar território divergente antes de congelar a investigação no ciclo. */
  @Test
  void validatesInvestigationScopeBeforeBindingItToTheCycle() {
    when(investigationService.ensureForProductDiscovery(
            "workspace-001", "viagem solo mulheres", "BR", "INSTAGRAM"))
        .thenReturn(investigation(19L, 0, "US"));

    assertThatThrownBy(
            () ->
                service.requestAndSearch(
                    89L,
                    new ProductDiscoveryMetaAdEvidenceRequest(
                        "lease-89", "viagem solo mulheres", "BR", "INSTAGRAM", 25)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("território e plataforma");
    Mockito.verify(sessionLinkService, Mockito.never())
        .bindActiveInvestigation(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
  }

  /** Deve pedir o Chromium público quando a API comercial brasileira não cobre a consulta. */
  @Test
  void requestsPublicBrowserBeforeHumanFallback() {
    insertInvestigation(9L, "BR");
    MoisMetaAdDtos.InvestigationResponse investigation = investigation(9L, 0);
    when(investigationService.ensureForProductDiscovery(
            "workspace-001", "guarda roupa climatério", "BR", "INSTAGRAM"))
        .thenReturn(investigation);
    when(sessionLinkService.bindActiveInvestigation(85L, 9L, "lease-85")).thenReturn(investigation);

    ProductDiscoveryMetaAdEvidenceListResponse response =
        service.requestAndSearch(
            85L,
            new ProductDiscoveryMetaAdEvidenceRequest(
                "lease-85", "guarda roupa climatério", "BR", "INSTAGRAM", 25));

    assertThat(response.sourceStatus()).isEqualTo("AWAITING_PUBLIC_BROWSER");
    assertThat(response.collectionMode()).isEqualTo("PUBLIC_BROWSER");
  }

  /** Deve diferenciar vazio verificado de bloqueio que ainda exige observação humana. */
  @Test
  void distinguishesVerifiedEmptyFromBrowserFallback() {
    insertInvestigation(10L, "BR");
    MoisMetaAdDtos.InvestigationResponse investigation = investigation(10L, 0);
    when(investigationService.ensureForProductDiscovery(
            "workspace-001", "viagem solo mulheres", "BR", "INSTAGRAM"))
        .thenReturn(investigation);
    when(sessionLinkService.bindActiveInvestigation(86L, 10L, "lease-86"))
        .thenReturn(investigation);
    insertBrowserRun(86L, 10L, "EMPTY", true);

    ProductDiscoveryMetaAdEvidenceListResponse empty =
        service.requestAndSearch(
            86L,
            new ProductDiscoveryMetaAdEvidenceRequest(
                "lease-86", "viagem solo mulheres", "BR", "INSTAGRAM", 25));

    assertThat(empty.sourceStatus()).isEqualTo("NO_MATCHING_ACTIVE_ADS");
    assertThat(empty.collectionMode()).isEqualTo("PUBLIC_BROWSER");

    jdbcTemplate.update("DELETE FROM product_discovery_meta_browser_run WHERE cycle_id = 86");
    insertBrowserRun(86L, 10L, "FALLBACK_REQUIRED", false);
    ProductDiscoveryMetaAdEvidenceListResponse fallback =
        service.requestAndSearch(
            86L,
            new ProductDiscoveryMetaAdEvidenceRequest(
                "lease-86", "viagem solo mulheres", "BR", "INSTAGRAM", 25));

    assertThat(fallback.sourceStatus()).isEqualTo("AWAITING_SUPERVISED_OBSERVATION");
    assertThat(fallback.collectionMode()).isEqualTo("SUPERVISED");
  }

  /** Deve restringir a reanálise à investigação anterior mesmo quando o plano muda a consulta. */
  @Test
  void reusesOnlyTheLinkedSupervisedInvestigation() {
    Instant now = Instant.now();
    insertInvestigation(7L, "BR");
    insertInvestigation(8L, "BR");
    insertAsset(
        7L,
        "ad-shared",
        "Marca de outro mercado",
        "Moda festa casamento para convidadas",
        "[\"INSTAGRAM\"]",
        true,
        now.minusSeconds(3_600));
    insertAsset(
        8L,
        "ad-other-session",
        "Treino Entrevista",
        "Treino entrevista emprego para jovens",
        "[\"INSTAGRAM\"]",
        true,
        now.minusSeconds(3_600));
    insertSupervisedObservation(
        7L,
        7L,
        7L,
        "ad-shared",
        "Treino Entrevista",
        "Treino entrevista emprego para jovens",
        "INSTAGRAM",
        now.minusSeconds(3_600));
    insertSupervisedObservation(
        8L,
        8L,
        7L,
        "ad-shared",
        "Marca de outro mercado",
        "Moda festa casamento para convidadas",
        "INSTAGRAM",
        now.minusSeconds(1_800));
    insertSupervisedObservation(
        9L,
        8L,
        8L,
        "ad-other-session",
        "Treino Entrevista",
        "Treino entrevista emprego para jovens",
        "INSTAGRAM",
        now.minusSeconds(1_800));
    MoisMetaAdDtos.InvestigationResponse linked = investigation(7L, 1);
    when(sessionLinkService.linkedInvestigation(84L)).thenReturn(Optional.of(linked));
    when(sessionLinkService.bindActiveInvestigation(84L, 7L, "lease-84")).thenReturn(linked);

    ProductDiscoveryMetaAdEvidenceListResponse response =
        service.requestAndSearch(
            84L,
            new ProductDiscoveryMetaAdEvidenceRequest(
                "lease-84", "consulta reformulada pelo modelo", "BR", "INSTAGRAM", 25));

    assertThat(response.investigationId()).isEqualTo(7L);
    assertThat(response.query()).isEqualTo("treino entrevista emprego");
    assertThat(response.items())
        .extracting(ProductDiscoveryMetaAdEvidenceResponse::metaAdId)
        .containsExactly("ad-shared");
    assertThat(response.items().getFirst().advertiserName()).isEqualTo("Treino Entrevista");
    assertThat(response.items().getFirst().adTexts())
        .containsExactly("Treino entrevista emprego para jovens");
  }

  /** Cria as tabelas mínimas consultadas pelo serviço. */
  private void createSchema() {
    jdbcTemplate.execute(
        """
        CREATE TABLE mois_meta_ad_investigation (
          id BIGINT PRIMARY KEY,
          country_code VARCHAR(8) NOT NULL
        )
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE mois_meta_ad_asset (
          id BIGINT PRIMARY KEY,
          workspace_id VARCHAR(80) NOT NULL,
          meta_ad_id VARCHAR(120) NOT NULL,
          advertiser_name VARCHAR(255),
          ad_status VARCHAR(32) NOT NULL,
          publisher_platforms_json TEXT,
          format_types_json TEXT,
          ad_texts_json TEXT,
          destination_url VARCHAR(2048),
          snapshot_url VARCHAR(2048),
          page_active TINYINT NOT NULL,
          commercial_signal TINYINT NOT NULL,
          observation_count INT NOT NULL,
          first_observed_at DATETIME NOT NULL,
          last_observed_at DATETIME NOT NULL
        )
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE mois_meta_ad_observation (
          id BIGINT PRIMARY KEY,
          investigation_id BIGINT NOT NULL,
          asset_id BIGINT NOT NULL,
          observed_at DATETIME NOT NULL,
          raw_payload_json LONGTEXT
        )
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE product_discovery_meta_browser_run (
          id BIGINT AUTO_INCREMENT PRIMARY KEY,
          cycle_id BIGINT NOT NULL,
          investigation_id BIGINT NOT NULL,
          outcome VARCHAR(40) NOT NULL,
          platform_filter_confirmed TINYINT NOT NULL,
          result_count INT NOT NULL,
          finished_at DATETIME NOT NULL
        )
        """);
  }

  /** Insere uma investigação usada para delimitar o país observado. */
  private void insertInvestigation(long id, String country) {
    jdbcTemplate.update(
        "INSERT INTO mois_meta_ad_investigation (id, country_code) VALUES (?, ?)", id, country);
  }

  /** Insere um ativo com plataforma e instante controlados pelo cenário. */
  private void insertAsset(
      long id,
      String metaAdId,
      String advertiserName,
      String adText,
      String publisherPlatforms,
      boolean active,
      Instant lastObservedAt) {
    jdbcTemplate.update(
        """
        INSERT INTO mois_meta_ad_asset
          (id, workspace_id, meta_ad_id, advertiser_name, ad_status,
           publisher_platforms_json, format_types_json, ad_texts_json, destination_url,
           snapshot_url, page_active, commercial_signal, observation_count,
           first_observed_at, last_observed_at)
        VALUES (?, 'workspace-001', ?, ?, 'ACTIVE', ?, '[]', ?, ?, ?, ?, 1, 2, ?, ?)
        """,
        id,
        metaAdId,
        advertiserName,
        publisherPlatforms,
        "[\"" + adText + "\"]",
        "https://example.test/" + metaAdId,
        "https://www.facebook.com/ads/library/?id=" + metaAdId,
        active,
        Timestamp.from(lastObservedAt.minusSeconds(31L * 86_400L)),
        Timestamp.from(lastObservedAt));
  }

  /** Vincula um ativo à investigação e ao instante real de observação. */
  private void insertObservation(long id, long investigationId, long assetId, Instant observedAt) {
    jdbcTemplate.update(
        "INSERT INTO mois_meta_ad_observation (id, investigation_id, asset_id, observed_at) VALUES (?, ?, ?, ?)",
        id,
        investigationId,
        assetId,
        Timestamp.from(observedAt));
  }

  /** Registra um desfecho controlado da navegação para validar a semântica da cobertura. */
  private void insertBrowserRun(
      long cycleId, long investigationId, String outcome, boolean filtersConfirmed) {
    jdbcTemplate.update(
        """
        INSERT INTO product_discovery_meta_browser_run
          (cycle_id, investigation_id, outcome, platform_filter_confirmed, result_count, finished_at)
        VALUES (?, ?, ?, ?, 0, ?)
        """,
        cycleId,
        investigationId,
        outcome,
        filtersConfirmed,
        Timestamp.from(Instant.now()));
  }

  /** Vincula o snapshot humano exato usado para impedir mistura entre investigações. */
  private void insertSupervisedObservation(
      long id,
      long investigationId,
      long assetId,
      String adReference,
      String advertiserName,
      String adText,
      String publisherPlatform,
      Instant observedAt) {
    String rawPayload =
        """
        {"adReference":"%s","advertiserName":"%s","adLibraryUrl":"https://www.facebook.com/ads/library/?id=%s","adText":"%s","publisherPlatforms":["%s"],"formatType":"VIDEO","destinationUrl":"https://example.test/%s","pageActive":true,"commercialSignal":true}
        """
            .formatted(
                adReference, advertiserName, adReference, adText, publisherPlatform, adReference)
            .trim();
    jdbcTemplate.update(
        "INSERT INTO mois_meta_ad_observation (id, investigation_id, asset_id, observed_at, raw_payload_json) VALUES (?, ?, ?, ?, ?)",
        id,
        investigationId,
        assetId,
        Timestamp.from(observedAt),
        rawPayload);
  }

  /** Monta o contrato do radar devolvido à Descoberta sem depender do banco completo. */
  private MoisMetaAdDtos.InvestigationResponse investigation(long id, int adsObserved) {
    return investigation(id, adsObserved, "BR");
  }

  /** Monta uma investigação com território explícito para validar segregação do ciclo. */
  private MoisMetaAdDtos.InvestigationResponse investigation(
      long id, int adsObserved, String country) {
    Instant now = Instant.now();
    return new MoisMetaAdDtos.InvestigationResponse(
        id,
        "workspace-001",
        "treino entrevista emprego",
        country,
        "INSTAGRAM",
        "ACTIVE_SUPERVISED",
        new MoisMetaAdDtos.CollectionState(
            "SUPERVISED",
            "Observação oficial supervisionada",
            "https://www.facebook.com/ads/library/?country=BR&q=entrevista",
            now),
        "INVESTIGAR",
        List.of(),
        List.of(),
        MoisMetaAdDtos.EthicalModelingCard.empty(),
        MoisMetaAdDtos.CreativeIntelligenceBrief.unavailable(),
        adsObserved,
        now,
        now);
  }
}
