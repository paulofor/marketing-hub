package com.marketinghub.businessprocess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Comprova que critérios comerciais novos preservam contratos, histórico e isolamento no MySQL 5.7.
 */
@EnabledIfEnvironmentVariable(named = "PDE_PRINCIPLES_MYSQL", matches = "true")
class PdeCommercialPrinciplesMysql57Test {
  private static final Path FIXTURE = Path.of("../../infra/testing/pde-commercial-principles");
  private static final String CHANGE =
      "db/changelog/changesets/2026-09-22-pde-commercial-principles-v1.yaml";
  private static final String GAP_DEEPENING_CHANGE =
      "db/changelog/changesets/2026-09-23-product-discovery-gap-deepening-v1.yaml";
  private static final String SAFIRA_CHANGE =
      "db/changelog/changesets/2026-09-23-safira-commercial-preparation-v1.yaml";
  private final ObjectMapper mapper = new ObjectMapper();

  /** Aplica a revisão, recusa fontes inválidas e conserva toda evidência nas reaplicações. */
  @Test
  void versionsObjectivesWithoutRewritingHistoryOrExecutionContracts() throws Exception {
    String host = System.getenv().getOrDefault("PDE_PRINCIPLES_DB_HOST", "127.0.0.1");
    assertThat(host).isIn("127.0.0.1", "sandbox-docker");
    try (var connection = openConnection(host)) {
      assertThat(connection.getMetaData().getDatabaseProductVersion()).startsWith("5.7.");
      var sources = loadSources();
      initialize(connection, sources);
      String history =
          scalar(
              connection,
              "SELECT CONCAT(source_reference,':',status,':',estimated_cost_usd,':',result_json) FROM agent_task");
      String productChain = scalar(connection, "SELECT chain_definition_id FROM product");
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var migration = new Liquibase(CHANGE, new ClassLoaderResourceAccessor(), database)) {
        // A ausência da fonte não pode criar uma cadeia parcial.
        execute(
            connection,
            "UPDATE business_process_definition SET version_number=99 WHERE process_code='pde-commercial-plan-offer'");
        assertThatThrownBy(() -> migration.update("")).hasMessageContaining("Precondition failed");
        assertThat(scalar(connection, "SELECT COUNT(*) FROM business_process_chain_definition"))
            .isEqualTo("1");
        execute(
            connection,
            "UPDATE business_process_definition SET version_number=6 WHERE process_code='pde-commercial-plan-offer'");
        // Uma versão ocupada por outra entrega nunca é sobrescrita.
        execute(
            connection,
            "UPDATE business_process_definition SET version_number=7 WHERE process_code='pde-commercial-plan-offer'");
        insertSource(connection, sources.get(0));
        assertThatThrownBy(() -> migration.update("")).hasMessageContaining("Precondition failed");
        execute(
            connection,
            "DELETE FROM business_process_definition WHERE process_code='pde-commercial-plan-offer' AND version_number=6");
        execute(
            connection,
            "UPDATE business_process_definition SET version_number=6 WHERE process_code='pde-commercial-plan-offer'");
        migration.update("");
        verify(connection, sources);
        String activities =
            scalar(connection, "SELECT COUNT(*) FROM business_process_activity_definition");
        migration.update("");
        migration.rollback(1, "");
        assertThat(
                scalar(
                    connection,
                    "SELECT status FROM business_process_chain_definition WHERE version_number=17"))
            .isEqualTo("PUBLISHED");
        assertThat(
                scalar(
                    connection,
                    "SELECT status FROM business_process_chain_definition WHERE version_number=18"))
            .isEqualTo("RETIRED");
        migration.update("");
        verify(connection, sources);
        assertThat(scalar(connection, "SELECT COUNT(*) FROM business_process_activity_definition"))
            .isEqualTo(activities);
        assertThat(
                scalar(
                    connection,
                    "SELECT CONCAT(source_reference,':',status,':',estimated_cost_usd,':',result_json) FROM agent_task"))
            .isEqualTo(history);
        assertThat(scalar(connection, "SELECT chain_definition_id FROM product"))
            .isEqualTo(productChain);
        verifyGapDeepeningMigration(host, productChain);
        verifySafiraMigration(host, productChain);
        exportForBrowser(connection, sources);
        verifyPublicEvidenceMigration(host, productChain);
      }
    }
  }

  /**
   * Aplica a etapa comportamental, comprova retomada após ledger ausente e preserva a cadeia v18.
   */
  private void verifyGapDeepeningMigration(String host, String productChain) throws Exception {
    try (var connection = openConnection(host)) {
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var migration =
          new Liquibase(GAP_DEEPENING_CHANGE, new ClassLoaderResourceAccessor(), database)) {
        migration.update("");
        verifyGapDeepeningState(connection, productChain);

        execute(
            connection,
            "DELETE FROM DATABASECHANGELOG WHERE ID='2026-09-23-product-discovery-gap-deepening-v1-01-customer-interview-table'");
        migration.update("");
        assertThat(
                scalar(
                    connection,
                    "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID LIKE '2026-09-23-product-discovery-gap-deepening-v1-%'"))
            .isEqualTo("2");

        execute(
            connection,
            "DELETE activity FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.process_code='pde-opportunity-discovery' AND process.version_number=7 AND activity.activity_id='candidateGapDeepening'");
        execute(
            connection,
            "DELETE item FROM business_process_chain_item item JOIN business_process_chain_definition chain_definition ON chain_definition.id=item.chain_definition_id WHERE chain_definition.chain_code='pde-value-creation-delivery' AND chain_definition.version_number=19 AND item.sequence_number=1");
        execute(
            connection,
            "DELETE FROM DATABASECHANGELOG WHERE ID='2026-09-23-product-discovery-gap-deepening-v1-02-process-and-chain'");
        migration.update("");
        verifyGapDeepeningState(connection, productChain);

        migration.rollback(2, "");
        assertThat(
                scalar(
                    connection,
                    "SELECT CONCAT((SELECT status FROM business_process_definition WHERE process_code='pde-opportunity-discovery' AND version_number=6),':',(SELECT status FROM business_process_definition WHERE process_code='pde-opportunity-discovery' AND version_number=7),':',(SELECT status FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=18),':',(SELECT status FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=19),':',(SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product_discovery_customer_interview'))"))
            .isEqualTo("PUBLISHED:RETIRED:PUBLISHED:RETIRED:0");

        migration.update("");
        verifyGapDeepeningState(connection, productChain);
      }
    }
  }

  /** Confere política legada, novas versões e rollback sem apagar escolhas ou fontes históricas. */
  private void verifyPublicEvidenceMigration(String host, String productChain) throws Exception {
    try (var connection = openConnection(host)) {
      execute(connection, "CREATE TABLE IF NOT EXISTS agent(id BIGINT PRIMARY KEY, agent_key VARCHAR(100), current_version INT, status VARCHAR(30), model_name VARCHAR(100))");
      execute(connection, "CREATE TABLE IF NOT EXISTS agent_version(id BIGINT AUTO_INCREMENT PRIMARY KEY, agent_id BIGINT, version_number INT, contract_snapshot LONGTEXT, created_at DATETIME, UNIQUE KEY uk_av(agent_id,version_number))");
      execute(connection, "DELETE FROM agent_version");
      execute(connection, "DELETE FROM agent");
      execute(connection, "INSERT INTO agent VALUES(801,'market-radar',6,'ACTIVE','synthetic-model')");
      String original =
          scalar(
              connection,
              "SELECT diagram_json FROM business_process_definition WHERE process_code='pde-opportunity-discovery' AND version_number=7");
      execute(connection, "INSERT INTO product_discovery_cycle(id) VALUES(987)");
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var migration =
          new Liquibase(
              "db/changelog/changesets/2026-09-24-product-discovery-public-evidence-v1.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {
        var agentMigration = new Liquibase("db/changelog/changesets/2026-09-24-argos-agent-version-v7-public-evidence.yaml", new ClassLoaderResourceAccessor(), database);
        agentMigration.update("");
        assertThat(scalar(connection, "SELECT current_version FROM agent WHERE id=801")).isEqualTo("7");
        agentMigration.rollback(1, "");
        assertThat(scalar(connection, "SELECT current_version FROM agent WHERE id=801")).isEqualTo("6");
        agentMigration.update("");
        migration.update("");
        assertThat(
                scalar(
                    connection, "SELECT evidence_policy FROM product_discovery_cycle WHERE id=987"))
            .isEqualTo("CONSENTED_INTERVIEWS_V1");
        assertThat(
                scalar(
                    connection,
                    "SELECT COUNT(*) FROM business_process_chain_item i JOIN business_process_chain_definition c ON c.id=i.chain_definition_id WHERE c.version_number=21"))
            .isEqualTo("6");
        assertThat(
                scalar(
                    connection,
                    "SELECT COUNT(*) FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='pde-opportunity-discovery' AND p.version_number=8"))
            .isEqualTo("2");
        execute(
            connection,
            "UPDATE product_discovery_cycle SET evidence_policy='PUBLIC_SOURCES_V1' WHERE id=987");
        migration.update("");
        migration.rollback(2, "");
        assertThat(
                scalar(
                    connection, "SELECT evidence_policy FROM product_discovery_cycle WHERE id=987"))
            .isEqualTo("PUBLIC_SOURCES_V1");
        migration.update("");
        assertThat(
                scalar(
                    connection,
                    "SELECT diagram_json FROM business_process_definition WHERE process_code='pde-opportunity-discovery' AND version_number=7"))
            .isEqualTo(original);
        assertThat(scalar(connection, "SELECT chain_definition_id FROM product"))
            .isEqualTo(productChain);
      }
    }
  }

  /** Versiona Processo 5 e cadeia sem promover a validação privada nem reescrever o histórico. */
  private void verifySafiraMigration(String host, String productChain) throws Exception {
    try (var connection = openConnection(host)) {
      String historicalParent =
          scalar(
              connection,
              "SELECT diagram_json FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=8");
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var migration =
          new Liquibase(SAFIRA_CHANGE, new ClassLoaderResourceAccessor(), database)) {
        migration.update("");
        verifySafiraState(connection, productChain, historicalParent);
        String activityCount =
            scalar(
                connection,
                "SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.process_code IN ('safira-commercial-preparation-v1','pde-commercial-homologation-activation') AND process.version_number IN (1,9)");
        migration.update("");
        assertThat(
                scalar(
                    connection,
                    "SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.process_code IN ('safira-commercial-preparation-v1','pde-commercial-homologation-activation') AND process.version_number IN (1,9)"))
            .isEqualTo(activityCount);

        migration.rollback(1, "");
        assertThat(
                scalar(
                    connection,
                    "SELECT CONCAT((SELECT status FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=8),':',(SELECT status FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=9),':',(SELECT status FROM business_process_definition WHERE process_code='safira-commercial-preparation-v1' AND version_number=1),':',(SELECT status FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=19),':',(SELECT status FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=20))"))
            .isEqualTo("PUBLISHED:RETIRED:RETIRED:PUBLISHED:RETIRED");

        migration.update("");
        verifySafiraState(connection, productChain, historicalParent);
      }
    }
  }

  /** Confere rota tipada, atividades, cadeia e imutabilidade da versão comercial anterior. */
  private void verifySafiraState(
      Connection connection, String productChain, String historicalParent) throws Exception {
    assertThat(
            scalar(
                connection,
                "SELECT CONCAT((SELECT status FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=8),':',(SELECT status FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=9),':',(SELECT status FROM business_process_definition WHERE process_code='safira-commercial-preparation-v1' AND version_number=1),':',(SELECT JSON_LENGTH(JSON_EXTRACT(diagram_json,'$.nodes[1].subprocessRoutes')) FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=9),':',(SELECT COUNT(*) FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.process_code='safira-commercial-preparation-v1' AND process.version_number=1))"))
        .isEqualTo("RETIRED:PUBLISHED:PUBLISHED:3:5");
    assertThat(
            scalar(
                connection,
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(diagram_json,'$.nodes[1].subprocessRoutes[2].subprocessCode')) FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=9"))
        .isEqualTo("safira-commercial-preparation-v1");
    assertThat(
            scalar(
                connection,
                "SELECT CONCAT((SELECT status FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=19),':',(SELECT status FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=20),':',(SELECT COUNT(*) FROM business_process_chain_item item JOIN business_process_chain_definition chain_definition ON chain_definition.id=item.chain_definition_id WHERE chain_definition.chain_code='pde-value-creation-delivery' AND chain_definition.version_number=20),':',(SELECT COUNT(*) FROM business_process_chain_item item JOIN business_process_chain_definition chain_definition ON chain_definition.id=item.chain_definition_id JOIN business_process_definition process ON process.id=item.process_definition_id WHERE chain_definition.chain_code='pde-value-creation-delivery' AND chain_definition.version_number=20 AND process.process_code='pde-commercial-homologation-activation' AND process.version_number=9))"))
        .isEqualTo("RETIRED:PUBLISHED:6:1");
    assertThat(
            scalar(
                connection,
                "SELECT diagram_json FROM business_process_definition WHERE process_code='pde-commercial-homologation-activation' AND version_number=8"))
        .isEqualTo(historicalParent);
    assertThat(scalar(connection, "SELECT chain_definition_id FROM product"))
        .isEqualTo(productChain);
  }

  /** Abre uma conexão exclusiva para que o fechamento do Liquibase não invalide outras provas. */
  private Connection openConnection(String host) throws Exception {
    return DriverManager.getConnection(
        "jdbc:mysql://"
            + host
            + ":33418/principles_test?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
        "root",
        "principles-local-only");
  }

  /** Confere schema, duas atividades, versão publicada e cadeia sem trocar o produto histórico. */
  private void verifyGapDeepeningState(Connection connection, String productChain)
      throws Exception {
    assertThat(
            scalar(
                connection,
                "SELECT CONCAT((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product_discovery_customer_interview'),':',(SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product_discovery_customer_interview' AND COLUMN_NAME IN ('consent_captured_at','created_at','updated_at') AND DATA_TYPE='datetime' AND DATETIME_PRECISION=6 AND IS_NULLABLE='NO'),':',(SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME='product_discovery_customer_interview'),':',(SELECT COUNT(DISTINCT INDEX_NAME) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='product_discovery_customer_interview' AND INDEX_NAME IN ('uk_pd_customer_interview_cycle_code','idx_pd_customer_interview_opportunity','idx_pd_customer_interview_cycle_outcome')))"))
        .isEqualTo("16:3:2:3");
    assertThat(
            scalar(
                connection,
                "SELECT CONCAT((SELECT status FROM business_process_definition WHERE process_code='pde-opportunity-discovery' AND version_number=6),':',(SELECT status FROM business_process_definition WHERE process_code='pde-opportunity-discovery' AND version_number=7),':',(SELECT JSON_UNQUOTE(JSON_EXTRACT(diagram_json,'$.researchContractVersion')) FROM business_process_definition WHERE process_code='pde-opportunity-discovery' AND version_number=7),':',(SELECT GROUP_CONCAT(activity.activity_id ORDER BY activity.activity_id SEPARATOR ',') FROM business_process_activity_definition activity JOIN business_process_definition process ON process.id=activity.process_definition_id WHERE process.process_code='pde-opportunity-discovery' AND process.version_number=7))"))
        .isEqualTo(
            "RETIRED:PUBLISHED:CANDIDATE_GAP_DEEPENING_V1:candidateGapDeepening,marketEvidence");
    assertThat(
            scalar(
                connection,
                "SELECT CONCAT((SELECT status FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=18),':',(SELECT status FROM business_process_chain_definition WHERE chain_code='pde-value-creation-delivery' AND version_number=19),':',(SELECT COUNT(*) FROM business_process_chain_item item JOIN business_process_chain_definition chain_definition ON chain_definition.id=item.chain_definition_id WHERE chain_definition.chain_code='pde-value-creation-delivery' AND chain_definition.version_number=19),':',(SELECT COUNT(*) FROM business_process_chain_item item JOIN business_process_chain_definition chain_definition ON chain_definition.id=item.chain_definition_id JOIN business_process_definition process ON process.id=item.process_definition_id WHERE chain_definition.chain_code='pde-value-creation-delivery' AND chain_definition.version_number=19 AND item.sequence_number=1 AND process.process_code='pde-opportunity-discovery' AND process.version_number=7))"))
        .isEqualTo("RETIRED:PUBLISHED:6:1");
    assertThat(scalar(connection, "SELECT chain_definition_id FROM product"))
        .isEqualTo(productChain);
  }

  /** Carrega definições canônicas, sem dados de produtos ou clientes produtivos. */
  private List<JsonNode> loadSources() throws Exception {
    List<JsonNode> values = new ArrayList<>();
    mapper.readTree(FIXTURE.resolve("sources.json").toFile()).forEach(values::add);
    mapper.readTree(FIXTURE.resolve("unchanged.json").toFile()).forEach(values::add);
    return values;
  }

  /**
   * Recria somente o banco isolado e registra vínculos sintéticos independentes dos IDs produtivos.
   */
  private void initialize(Connection connection, List<JsonNode> sources) throws Exception {
    for (String table :
        List.of(
            "product_discovery_customer_interview",
            "product_discovery_opportunity",
            "product_discovery_cycle",
            "agent_task",
            "product",
            "business_process_chain_item",
            "business_process_chain_definition",
            "business_process_activity_definition",
            "business_process_definition",
            "DATABASECHANGELOG",
            "DATABASECHANGELOGLOCK")) {
      execute(connection, "DROP TABLE IF EXISTS " + table);
    }
    for (String sql : Files.readString(FIXTURE.resolve("baseline.sql")).split(";")) {
      if (!sql.isBlank()) execute(connection, sql);
    }
    for (JsonNode source : sources) {
      insertSource(connection, source);
      insertOriginalActivities(connection, source);
    }
    execute(
        connection,
        "INSERT INTO business_process_chain_definition (chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at) VALUES ('pde-value-creation-delivery','Cadeia sintética','Valor','Venda entregue','Margem',17,'PUBLISHED',UTC_TIMESTAMP())");
    var codes =
        List.of(
            "pde-opportunity-discovery",
            "pde-commercial-plan-offer",
            "pde-construction-approval",
            "pde-communication-sales-journey",
            "pde-commercial-homologation-activation",
            "pde-sales-delivery-learning");
    for (int i = 0; i < codes.size(); i++) {
      execute(
          connection,
          "INSERT INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) SELECT c.id,p.id,"
              + (i + 1)
              + ",'Valor',UTC_TIMESTAMP() FROM business_process_chain_definition c JOIN business_process_definition p ON p.process_code='"
              + codes.get(i)
              + "'");
    }
    execute(
        connection, "INSERT INTO product SELECT 9003,id FROM business_process_chain_definition");
    execute(
        connection,
        "INSERT INTO agent_task SELECT 8005,id,'product:9003@synthetic','COMPLETED',0.123,'{\"evidence\":\"preservada\"}' FROM business_process_definition WHERE process_code='pde-commercial-plan-offer'");
  }

  /** Persiste a definição e suas atividades com os mesmos campos usados pelo catálogo. */
  private void insertSource(Connection connection, JsonNode source) throws Exception {
    try (var s =
        connection.prepareStatement(
            "INSERT INTO business_process_definition (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,technical_reference,process_type,parent_process_code,execution_scope,diagram_json,created_at) VALUES (?,?,?,?,?,?,?,'PUBLISHED',?,?,?,?,?,UTC_TIMESTAMP())")) {
      String[] fields = {
        "processCode",
        "name",
        "purpose",
        "ownerName",
        "triggerDescription",
        "outcomeDescription",
        "versionNumber",
        "technicalReference",
        "processType",
        "parentProcessCode",
        "executionScope"
      };
      for (int i = 0; i < fields.length; i++)
        s.setString(i + 1, source.path(fields[i]).asText(null));
      s.setString(12, source.path("diagram").toString());
      s.executeUpdate();
    }
  }

  /**
   * Materializa atividades antigas para comprovar sua preservação, inclusive metadados não
   * alterados.
   */
  private void insertOriginalActivities(Connection connection, JsonNode source) throws Exception {
    long processId =
        Long.parseLong(
            scalar(
                connection,
                "SELECT id FROM business_process_definition WHERE process_code='"
                    + source.path("processCode").asText()
                    + "'"));
    for (JsonNode node : source.path("diagram").path("nodes")) {
      if (!"TASK".equals(node.path("type").asText())) continue;
      try (var s =
          connection.prepareStatement(
              "INSERT INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,execution_resource_code,subprocess_code,definition_json,created_at) VALUES (?,?,?,?,?,?,?,?,UTC_TIMESTAMP())")) {
        s.setLong(1, processId);
        String[] fields = {
          "id", "label", "description", "owner", "executionResourceCode", "subprocessCode"
        };
        for (int i = 0; i < fields.length; i++)
          s.setString(i + 2, node.path(fields[i]).asText(null));
        s.setString(8, node.toString());
        s.executeUpdate();
      }
    }
  }

  /** Compara todos os metadados e fluxos, permitindo somente as 29 descrições aprovadas. */
  private void verify(Connection connection, List<JsonNode> sources) throws Exception {
    JsonNode expected = mapper.readTree(FIXTURE.resolve("expected.json").toFile());
    for (JsonNode source : sources) {
      String code = source.path("processCode").asText();
      int version = source.path("versionNumber").asInt();
      var original =
          mapper.readTree(
              scalar(
                  connection,
                  "SELECT diagram_json FROM business_process_definition WHERE process_code='"
                      + code
                      + "' AND version_number="
                      + version));
      assertThat(original).isEqualTo(source.path("diagram"));
      assertThat(
              scalar(
                  connection,
                  "SELECT status FROM business_process_definition WHERE process_code='"
                      + code
                      + "' AND version_number="
                      + version))
          .isEqualTo("PUBLISHED");
      for (JsonNode node : original.path("nodes")) {
        if (!"TASK".equals(node.path("type").asText())) continue;
        assertThat(
                mapper.readTree(
                    scalar(
                        connection,
                        "SELECT a.definition_json FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='"
                            + code
                            + "' AND p.version_number="
                            + version
                            + " AND a.activity_id='"
                            + node.path("id").asText()
                            + "'")))
            .isEqualTo(node);
      }
      if (!expected.has(code)) continue;
      ObjectNode actual =
          (ObjectNode)
              mapper.readTree(
                  scalar(
                      connection,
                      "SELECT diagram_json FROM business_process_definition WHERE process_code='"
                          + code
                          + "' AND version_number="
                          + (version + 1)));
      if ("pde-construction-approval".equals(code))
        verifyExecutionCompatibility(code, version + 1, actual);
      assertThat(actual.remove("commercialCriteriaVersion").asText())
          .isEqualTo("PDE_COMMERCIAL_PRINCIPLES_V1");
      JsonNode objectives = expected.path(code).path("objectives");
      for (int i = 0; i < actual.path("nodes").size(); i++) {
        ObjectNode node = (ObjectNode) actual.path("nodes").get(i);
        String id = node.path("id").asText();
        if ("TASK".equals(node.path("type").asText())) {
          JsonNode row =
              mapper.readTree(
                  scalar(
                      connection,
                      "SELECT a.definition_json FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='"
                          + code
                          + "' AND p.version_number="
                          + (version + 1)
                          + " AND a.activity_id='"
                          + id
                          + "'"));
          assertThat(row).isEqualTo(node);
          assertThat(
                  scalar(
                      connection,
                      "SELECT a.objective FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.process_code='"
                          + code
                          + "' AND p.version_number="
                          + (version + 1)
                          + " AND a.activity_id='"
                          + id
                          + "'"))
              .isEqualTo(node.path("description").asText());
        }
        if (objectives.has(id)) {
          assertThat(node.path("description").asText())
              .isEqualTo(objectives.path(id).asText())
              .contains("Entregar:", "Aceite:", "Medir:");
          node.set("description", original.path("nodes").get(i).path("description"));
        }
      }
      assertThat(actual).isEqualTo(original);
    }
    assertThat(
            scalar(
                connection,
                "SELECT COUNT(*) FROM business_process_chain_item i JOIN business_process_chain_definition c ON c.id=i.chain_definition_id WHERE c.version_number=18"))
        .isEqualTo("6");
    assertThat(
            scalar(
                connection,
                "SELECT COUNT(*) FROM business_process_chain_item i JOIN business_process_chain_definition c ON c.id=i.chain_definition_id JOIN business_process_definition p ON p.id=i.process_definition_id WHERE c.version_number=18 AND p.status='PUBLISHED'"))
        .isEqualTo("6");
  }

  /** Confere que a definição materializada continua reconhecida pelo gate e pelo retrabalho. */
  private void verifyExecutionCompatibility(String code, int version, JsonNode diagram) {
    var process = new BusinessProcessDefinition();
    process.setProcessCode(code);
    process.setVersionNumber(version);
    process.setDiagramJson(diagram.toString());
    var activity = new BusinessProcessActivityDefinition();
    activity.setActivityId("agentValidationGate");
    var executor =
        new com.marketinghub.product.service.agentvalidation.PdeAgentValidationGateActivityExecutor(
            null, null, null, null, mapper);
    assertThat(executor.supports(process, activity)).isTrue();
    activity.setActivityId("prototypeCorrection");
    var readiness =
        new com.marketinghub.product.service.agentvalidation
            .PdeAgentValidationReworkReadinessProvider(null, null, mapper);
    assertThat(readiness.supports(process, activity)).isTrue();
  }

  /** Exporta as respostas pós-migração para homologar a interface com dependências simuladas. */
  private void exportForBrowser(Connection connection, List<JsonNode> sources) throws Exception {
    var result = mapper.createArrayNode();
    var expected = mapper.readTree(FIXTURE.resolve("expected.json").toFile());
    for (JsonNode source : sources) {
      ObjectNode value = source.deepCopy();
      String code = source.path("processCode").asText();
      int version = source.path("versionNumber").asInt() + (expected.has(code) ? 1 : 0);
      String where = " WHERE process_code='" + code + "' AND version_number=" + version;
      value.put(
          "id",
          Long.parseLong(scalar(connection, "SELECT id FROM business_process_definition" + where)));
      value.put("versionNumber", version).put("status", "PUBLISHED");
      value.set(
          "diagram",
          mapper.readTree(
              scalar(connection, "SELECT diagram_json FROM business_process_definition" + where)));
      result.add(value);
    }
    Files.createDirectories(Path.of("target/principles"));
    mapper
        .writerWithDefaultPrettyPrinter()
        .writeValue(Path.of("target/principles/processes.json").toFile(), result);
  }

  /** Executa somente SQL da fixture no banco descartável. */
  private void execute(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  /** Obtém uma célula para verificar a persistência real. */
  private String scalar(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var result = statement.executeQuery(sql)) {
      assertThat(result.next()).isTrue();
      return result.getString(1);
    }
  }
}
