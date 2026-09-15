package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.PdeProductionSlot;
import com.marketinghub.product.mapper.ProductMapper;
import com.marketinghub.product.service.ProductScientificArticleService;
import com.marketinghub.product.service.ProductService;
import com.marketinghub.product.web.PdePublicProductController;
import com.marketinghub.product.web.ProductController;
import com.marketinghub.repository.jpa.experiment.video.ExperimentVideoAssetRepository;
import com.marketinghub.repository.jpa.pde.PdeProductionSlotRepository;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Optional;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Comprova restauração histórica e isolamento do contrato público em MySQL 5.7 descartável. */
@EnabledIfEnvironmentVariable(named = "PDE_CONTRACT_MYSQL", matches = "local")
class PdePublishedContractMysql57Test {
  private static final String SLUG = "metodo-musa-7-dias";
  private static final String VERSION = "musa-pde-entry-v5-video-explicativo";
  private static final String REPAIR = "2026-09-15-musa-v5-published-contract-repair.yaml";
  private final ObjectMapper mapper = new ObjectMapper();

  /** Reproduz o histórico, aplica o reparo e valida HTTP, reaplicação e rollback conservador. */
  @Test
  void restoresHistoricalV5WithoutChangingOtherVersions() throws Exception {
    try (var connection = connection();
        var statement = connection.createStatement()) {
      assertThat(connection.getMetaData().getDatabaseProductVersion()).startsWith("5.7.");
      statement.execute(
          "DROP TABLE IF EXISTS DATABASECHANGELOGLOCK, DATABASECHANGELOG, product, pde_production_slot");
      statement.execute(
          "CREATE TABLE product (slug VARCHAR(191) PRIMARY KEY, scientific_evidence_pack LONGTEXT, updated_at DATETIME NOT NULL)");
      statement.execute("INSERT INTO product VALUES ('" + SLUG + "', NULL, '2026-07-20 00:00:00')");
      statement.execute(
          "CREATE TABLE pde_production_slot (id BIGINT PRIMARY KEY, product_slug VARCHAR(191), slot_code VARCHAR(32), experience_version VARCHAR(191), layout_key VARCHAR(100), published_experience_json LONGTEXT, draft_experience_json LONGTEXT, published_by VARCHAR(191), published_at DATETIME NULL, updated_at DATETIME NOT NULL, source_experiment_id BIGINT, status VARCHAR(20))");
      statement.execute(
          "INSERT INTO pde_production_slot VALUES (3, '"
              + SLUG
              + "', 'v5', '"
              + VERSION
              + "', 'video-explicativo', NULL, 'rascunho-preservado', NULL, NULL, '2026-07-30 00:00:00', 74, 'ACTIVE')");
      statement.execute(
          "INSERT INTO pde_production_slot VALUES (4, '"
              + SLUG
              + "', 'v6', 'v6', 'video-motivacional', '{\"slug\":\"metodo-musa-7-dias\",\"experienceVersion\":\"v6\"}', 'v6-draft', 'human', '2026-08-01 00:00:00', '2026-08-01 00:00:00', 76, 'ACTIVE')");
      statement.execute(
          "INSERT INTO pde_production_slot SELECT 5, 'another-product', slot_code, experience_version, layout_key, NULL, NULL, NULL, NULL, updated_at, 99, status FROM pde_production_slot WHERE id=3");
      statement.execute(
          "INSERT INTO pde_production_slot SELECT 6, product_slug, 'v7', 'v7', 'espelho-antes-de-sair', '{\"slug\":\"metodo-musa-7-dias\",\"experienceVersion\":\"v7\"}', 'v7-draft', 'human', published_at, updated_at, 90, status FROM pde_production_slot WHERE id=4");
    }
    for (String source :
        new String[] {
          "2026-07-21-product-pde-experience-json.yaml",
          "2026-07-21-product-musa-pde-single-action-entry.yaml",
          "2026-07-21-product-musa-pde-experience-version.yaml",
          "2026-07-26-product-musa-pde-video-explainer-entry.yaml",
          "2026-07-28-musa-v6-approved-hero-video.yaml",
          "2026-07-28-musa-v6-video-audio-mobile.yaml"
        }) {
      migrate(source, false);
    }
    String historical;
    String preservedV6;
    try (var connection = connection();
        var statement = connection.createStatement()) {
      historical =
          value(
              connection,
              "SELECT JSON_SET(pde_experience_json, '$.layoutKey', 'video-explicativo') FROM product");
      preservedV6 =
          value(connection, "SELECT published_experience_json FROM pde_production_slot WHERE id=4");
      statement.execute(
          "UPDATE product SET pde_experience_json='{\"slug\":\"metodo-musa-7-dias\",\"experienceVersion\":\"v7\"}'");
    }
    migrate(REPAIR, false);
    assertPublicContract(historical);
    migrate(REPAIR, false);
    try (var connection = connection();
        var statement = connection.createStatement()) {
      assertThat(
              value(
                  connection,
                  "SELECT published_experience_json FROM pde_production_slot WHERE id=3"))
          .isEqualTo(historical);
      assertThat(
              value(connection, "SELECT draft_experience_json FROM pde_production_slot WHERE id=3"))
          .isEqualTo("rascunho-preservado");
      assertThat(
              value(
                  connection,
                  "SELECT published_experience_json FROM pde_production_slot WHERE id=4"))
          .isEqualTo(preservedV6);
      assertThat(
              value(
                  connection,
                  "SELECT published_experience_json FROM pde_production_slot WHERE id=5"))
          .isNull();
      assertThat(
              value(
                  connection,
                  "SELECT JSON_UNQUOTE(JSON_EXTRACT(pde_experience_json, '$.experienceVersion')) FROM product"))
          .isEqualTo("v7");
      assertThat(
              value(
                  connection,
                  "SELECT CONCAT(status, ':', source_experiment_id) FROM pde_production_slot WHERE id=3"))
          .isEqualTo("ACTIVE:74");
      assertThat(
              value(
                  connection,
                  "SELECT published_experience_json FROM pde_production_slot WHERE id=6"))
          .isEqualTo("{\"slug\":\"metodo-musa-7-dias\",\"experienceVersion\":\"v7\"}");
      statement.execute(
          "DELETE FROM DATABASECHANGELOG WHERE ID='2026-09-15-musa-v5-published-contract-repair-001'");
    }
    migrate(REPAIR, false);
    assertPublicContract(historical);
    migrate(REPAIR, true);
    try (var connection = connection()) {
      assertThat(
              value(
                  connection,
                  "SELECT published_experience_json FROM pde_production_slot WHERE id=3"))
          .isNull();
      assertThat(
              value(connection, "SELECT draft_experience_json FROM pde_production_slot WHERE id=3"))
          .isEqualTo("rascunho-preservado");
    }
    migrate(REPAIR, false);
    try (var connection = connection();
        var statement = connection.createStatement()) {
      statement.execute(
          "UPDATE pde_production_slot SET published_experience_json='{\"custom\":true}', published_by='migration:2026-09-15-v5-snapshot' WHERE id=3");
    }
    migrate(REPAIR, true);
    migrate(REPAIR, false);
    try (var connection = connection()) {
      assertThat(
              value(
                  connection,
                  "SELECT published_experience_json FROM pde_production_slot WHERE id=3"))
          .isEqualTo("{\"custom\":true}");
    }
    String export = System.getenv("PDE_CONTRACT_FIXTURE_EXPORT");
    if (export != null && !export.isBlank()) {
      Files.writeString(Path.of(export), historical);
    }
  }

  /** Lê o snapshot do banco e exercita os dois controllers reais com o resolvedor real. */
  private void assertPublicContract(String expected) throws Exception {
    PdeProductionSlot slot;
    try (var connection = connection()) {
      slot =
          PdeProductionSlot.builder()
              .productSlug(SLUG)
              .slotCode("v5")
              .experienceVersion(VERSION)
              .publishedExperienceJson(
                  value(
                      connection,
                      "SELECT published_experience_json FROM pde_production_slot WHERE id=3"))
              .build();
    }
    var repository = mock(PdeProductionSlotRepository.class);
    when(repository.findByProductSlugAndSlotCode(SLUG, "v5")).thenReturn(Optional.of(slot));
    when(repository.findFirstByProductSlugAndExperienceVersionOrderByPublishedAtDesc(SLUG, VERSION))
        .thenReturn(Optional.of(slot));
    var service =
        new PdeProductionSlotService(
            repository,
            mock(ExperimentVideoAssetRepository.class),
            HttpClient.newHttpClient(),
            mapper);
    var products = mock(ProductService.class);
    var mvc =
        MockMvcBuilders.standaloneSetup(
                new PdePublicProductController(products, service),
                new ProductController(
                    products,
                    mock(ProductScientificArticleService.class),
                    mock(ProductMapper.class),
                    service))
            .build();
    for (String route :
        new String[] {
          "/api/pde/products/" + SLUG, "/api/products/public/" + SLUG + "/pde-experience"
        }) {
      for (String selector : new String[] {"slotCode", "experienceVersion"}) {
        String response =
            mvc.perform(get(route).param(selector, selector.equals("slotCode") ? "v5" : VERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.experienceVersion").value(VERSION))
                .andReturn()
                .getResponse()
                .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(mapper.readTree(response)).isEqualTo(mapper.readTree(expected));
      }
    }
  }

  /** Usa somente banco descartável com credencial sintética e host explicitamente local. */
  private Connection connection() throws Exception {
    String host = System.getenv().getOrDefault("PDE_CONTRACT_MYSQL_HOST", "127.0.0.1");
    assertThat(host).isIn("127.0.0.1", "localhost", "sandbox-docker");
    String port = System.getenv().getOrDefault("PDE_CONTRACT_MYSQL_PORT", "33385");
    return DriverManager.getConnection(
        "jdbc:mysql://"
            + host
            + ":"
            + port
            + "/pde_contract_qa?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
        "root",
        "pde-contract-local-only");
  }

  /** Executa o arquivo Liquibase versionado ou seu rollback na mesma base isolada. */
  private void migrate(String changelog, boolean rollback) throws Exception {
    var database =
        DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(connection()));
    try (var migration =
        new Liquibase(
            "db/changelog/changesets/" + changelog, new ClassLoaderResourceAccessor(), database)) {
      if (rollback) {
        migration.rollback(1, "");
      } else {
        migration.update("");
      }
    }
  }

  /** Obtém um valor escalar sem alterar os registros consultados. */
  private String value(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var rows = statement.executeQuery(sql)) {
      assertThat(rows.next()).isTrue();
      return rows.getString(1);
    }
  }
}
