package com.marketinghub.mois.metaads.v1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Protege a criação idempotente de investigações solicitadas pela Descoberta PDE. */
class MoisMetaAdInvestigationPersistenceTest {

  private JdbcTemplate jdbcTemplate;
  private MoisMetaAdInvestigationService service;

  /** Prepara o contrato persistente mínimo do radar Meta. */
  @BeforeEach
  void setUp() {
    DriverManagerDataSource dataSource =
        new DriverManagerDataSource(
            "jdbc:h2:mem:mois_meta_investigation_"
                + UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
            "sa",
            "");
    jdbcTemplate = new JdbcTemplate(dataSource);
    createSchema();
    service = new MoisMetaAdInvestigationService(jdbcTemplate, new ObjectMapper());
  }

  /** Deve reutilizar país, plataforma e termos equivalentes sem duplicar acompanhamento. */
  @Test
  void reusesInvestigationForTheSameProductDiscoveryCategory() {
    MoisMetaAdDtos.InvestigationResponse first =
        service.ensureForProductDiscovery(
            "workspace-001", "Treino entrevista emprego", "br", "instagram");
    MoisMetaAdDtos.InvestigationResponse second =
        service.ensureForProductDiscovery(
            "workspace-001", " treino entrevista emprego ", "BR", "INSTAGRAM");

    assertThat(second.id()).isEqualTo(first.id());
    assertThat(second.status()).isEqualTo("ACTIVE_SUPERVISED");
    assertThat(second.publisherPlatform()).isEqualTo("INSTAGRAM");
    assertThat(second.collection().searchUrl())
        .contains("country=BR", "q=Treino+entrevista+emprego")
        .doesNotContain("utm_");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mois_meta_ad_investigation", Integer.class))
        .isEqualTo(1);
  }

  /** Cria as tabelas necessárias para a escrita e a leitura completa da investigação. */
  private void createSchema() {
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
          evidence_json TEXT,
          gaps_json TEXT,
          ethical_modeling_json TEXT,
          creative_briefing_json TEXT,
          error_message TEXT,
          started_at DATETIME,
          finished_at DATETIME,
          next_run_at DATETIME,
          created_at DATETIME NOT NULL,
          updated_at DATETIME NOT NULL,
          PRIMARY KEY (id)
        )
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE mois_meta_ad_asset (
          id BIGINT NOT NULL AUTO_INCREMENT,
          PRIMARY KEY (id)
        )
        """);
    jdbcTemplate.execute(
        """
        CREATE TABLE mois_meta_ad_observation (
          id BIGINT NOT NULL AUTO_INCREMENT,
          investigation_id BIGINT NOT NULL,
          asset_id BIGINT NOT NULL,
          observed_at DATETIME NOT NULL,
          PRIMARY KEY (id)
        )
        """);
  }
}
