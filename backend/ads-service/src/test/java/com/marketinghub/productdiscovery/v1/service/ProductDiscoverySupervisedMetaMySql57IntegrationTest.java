package com.marketinghub.productdiscovery.v1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Responsabilidade: homologar a persistência da sessão de Argos no dialeto físico MySQL 5.7. */
@EnabledIfSystemProperty(named = "argos.meta.mysql57", matches = "true")
class ProductDiscoverySupervisedMetaMySql57IntegrationTest {

  private JdbcTemplate jdbcTemplate;
  private MoisMetaAdInvestigationService investigationService;
  private ProductDiscoveryMetaAdEvidenceService evidenceService;

  /** Recria somente as tabelas isoladas da homologação e configura os serviços reais. */
  @BeforeEach
  void setUp() {
    String url = System.getenv("ARGOS_META_MYSQL_URL");
    if (url == null || url.isBlank()) {
      throw new IllegalStateException("ARGOS_META_MYSQL_URL não foi informada");
    }
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(url, "marketinghub", "marketinghub-local");
    jdbcTemplate = new JdbcTemplate(dataSource);
    recreateSchema();
    ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    investigationService = new MoisMetaAdInvestigationService(jdbcTemplate, objectMapper);
    evidenceService =
        new ProductDiscoveryMetaAdEvidenceService(
            jdbcTemplate,
            investigationService,
            mock(ProductDiscoveryMetaAdSessionLinkService.class),
            objectMapper);
  }

  /** Persiste payload bruto, deduplica retry e exclui Facebook da cobertura Instagram. */
  @Test
  void persistsAndReadsOnlyTheLinkedInstagramEvidence() {
    MoisMetaAdDtos.InvestigationResponse investigation =
        investigationService.ensureForProductDiscovery(
            "workspace-001", "autocuidado feminino visual", "BR", "INSTAGRAM");
    Instant observedAt = Instant.now().minusSeconds(60);
    MoisMetaAdDtos.SupervisedObservationRequest instagramObservation =
        observation("ad-instagram", "INSTAGRAM", observedAt);

    MoisMetaAdDtos.ObservationBatchResponse first =
        investigationService.ingestSupervised(investigation.id(), instagramObservation);
    MoisMetaAdDtos.ObservationBatchResponse repeated =
        investigationService.ingestSupervised(investigation.id(), instagramObservation);
    investigationService.ingestSupervised(
        investigation.id(), observation("ad-facebook", "FACEBOOK", observedAt.plusSeconds(1)));
    MoisMetaAdDtos.InvestigationResponse otherInvestigation =
        investigationService.ensureForProductDiscovery(
            "workspace-001", "moda festa casamento", "BR", "INSTAGRAM");
    investigationService.ingestSupervised(
        otherInvestigation.id(),
        observation(
            "ad-instagram",
            "INSTAGRAM",
            observedAt.plusSeconds(2),
            "Moda para festa e casamento com entrega imediata."));

    MoisMetaAdDtos.InvestigationResponse refreshed =
        investigationService.get(investigation.id()).orElseThrow();
    ProductDiscoveryMetaAdEvidenceListResponse coverage =
        evidenceService.searchInvestigation(77L, refreshed, 50);

    assertThat(first.accepted()).isEqualTo(1);
    assertThat(repeated.accepted()).isZero();
    assertThat(refreshed.adsObserved()).isEqualTo(2);
    assertThat(coverage.sourceStatus()).isEqualTo("OBSERVED");
    assertThat(coverage.activeAds()).isEqualTo(1);
    assertThat(coverage.items())
        .extracting(ProductDiscoveryMetaAdEvidenceResponse::metaAdId)
        .containsExactly("ad-instagram");
    assertThat(coverage.items().getFirst().adTexts())
        .containsExactly("Autocuidado feminino visual em cinco minutos.");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT raw_payload_json FROM mois_meta_ad_observation WHERE investigation_id = ? AND collector_run_id LIKE 'supervised-%' ORDER BY id LIMIT 1",
                String.class, investigation.id()))
        .contains("ad-instagram", "INSTAGRAM", "Autocuidado feminino visual");
  }

  /** Monta uma observação oficial controlada para cada plataforma da homologação. */
  private MoisMetaAdDtos.SupervisedObservationRequest observation(
      String adReference, String publisherPlatform, Instant observedAt) {
    return observation(
        adReference,
        publisherPlatform,
        observedAt,
        "Autocuidado feminino visual em cinco minutos.");
  }

  /** Permite simular uma atualização do mesmo ativo por investigação concorrente. */
  private MoisMetaAdDtos.SupervisedObservationRequest observation(
      String adReference, String publisherPlatform, Instant observedAt, String adText) {
    return new MoisMetaAdDtos.SupervisedObservationRequest(
        adReference,
        "Marca observada",
        "https://business.facebook.com/ads/library/?id=" + adReference,
        adText,
        List.of(publisherPlatform),
        "VIDEO",
        null,
        "https://example.test/oferta",
        true,
        true,
        observedAt);
  }

  /** Cria o schema mínimo equivalente aos changelogs canônicos da investigação Meta. */
  private void recreateSchema() {
    jdbcTemplate.execute("DROP TABLE IF EXISTS mois_meta_ad_observation");
    jdbcTemplate.execute("DROP TABLE IF EXISTS mois_meta_ad_asset");
    jdbcTemplate.execute("DROP TABLE IF EXISTS mois_meta_ad_investigation");
    jdbcTemplate.execute(
        """
        CREATE TABLE mois_meta_ad_investigation (
          id BIGINT NOT NULL AUTO_INCREMENT,
          workspace_id VARCHAR(80) NOT NULL,
          search_terms VARCHAR(500) NOT NULL,
          country_code VARCHAR(8) NOT NULL DEFAULT 'BR',
          publisher_platform VARCHAR(32) NOT NULL DEFAULT 'INSTAGRAM',
          status VARCHAR(32) NOT NULL,
          gate_decision VARCHAR(24) NOT NULL DEFAULT 'INVESTIGAR',
          evidence_json LONGTEXT NULL,
          gaps_json LONGTEXT NULL,
          ethical_modeling_json LONGTEXT NULL,
          creative_briefing_json LONGTEXT NULL,
          error_message TEXT NULL,
          started_at DATETIME NULL,
          finished_at DATETIME NULL,
          next_run_at DATETIME NULL,
          created_at DATETIME NOT NULL,
          updated_at DATETIME NOT NULL,
          PRIMARY KEY (id),
          KEY idx_mois_meta_investigation_pending (status, created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE mois_meta_ad_asset (
          id BIGINT NOT NULL AUTO_INCREMENT,
          workspace_id VARCHAR(80) NOT NULL,
          meta_ad_id VARCHAR(120) NOT NULL,
          advertiser_id VARCHAR(120) NULL,
          advertiser_name VARCHAR(255) NULL,
          ad_status VARCHAR(32) NOT NULL,
          publisher_platforms_json TEXT NULL,
          format_types_json TEXT NULL,
          ad_texts_json LONGTEXT NULL,
          media_json LONGTEXT NULL,
          destination_url VARCHAR(2048) NULL,
          snapshot_url VARCHAR(2048) NULL,
          first_observed_at DATETIME NOT NULL,
          last_observed_at DATETIME NOT NULL,
          observation_count INT NOT NULL DEFAULT 1,
          page_active TINYINT(1) NOT NULL DEFAULT 0,
          commercial_signal TINYINT(1) NOT NULL DEFAULT 0,
          raw_payload_json LONGTEXT NOT NULL,
          created_at DATETIME NOT NULL,
          updated_at DATETIME NOT NULL,
          PRIMARY KEY (id),
          UNIQUE KEY uk_mois_meta_asset_workspace_ad (workspace_id, meta_ad_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE mois_meta_ad_observation (
          id BIGINT NOT NULL AUTO_INCREMENT,
          investigation_id BIGINT NOT NULL,
          asset_id BIGINT NOT NULL,
          collector_run_id VARCHAR(80) NOT NULL,
          observed_at DATETIME NOT NULL,
          raw_payload_json LONGTEXT NOT NULL,
          created_at DATETIME NOT NULL,
          PRIMARY KEY (id),
          UNIQUE KEY uk_mois_meta_observation_run_asset
            (investigation_id, collector_run_id, asset_id),
          KEY idx_mois_meta_observation_asset (asset_id, observed_at),
          CONSTRAINT fk_mois_meta_observation_investigation
            FOREIGN KEY (investigation_id) REFERENCES mois_meta_ad_investigation(id),
          CONSTRAINT fk_mois_meta_observation_asset
            FOREIGN KEY (asset_id) REFERENCES mois_meta_ad_asset(id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
        """);
  }
}
