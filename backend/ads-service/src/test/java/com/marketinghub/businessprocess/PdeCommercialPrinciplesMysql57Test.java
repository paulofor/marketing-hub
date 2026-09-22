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
  private final ObjectMapper mapper = new ObjectMapper();

  /** Aplica a revisão, recusa fontes inválidas e conserva toda evidência nas reaplicações. */
  @Test
  void versionsObjectivesWithoutRewritingHistoryOrExecutionContracts() throws Exception {
    String host = System.getenv().getOrDefault("PDE_PRINCIPLES_DB_HOST", "127.0.0.1");
    assertThat(host).isIn("127.0.0.1", "sandbox-docker");
    try (var connection =
        DriverManager.getConnection(
            "jdbc:mysql://"
                + host
                + ":33418/principles_test?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
            "root",
            "principles-local-only")) {
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
        exportForBrowser(connection, sources);
      }
    }
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
