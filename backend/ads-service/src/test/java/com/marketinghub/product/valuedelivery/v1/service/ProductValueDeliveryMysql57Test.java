package com.marketinghub.product.valuedelivery.v1.service;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Confere fisicamente a nova cadeia sem migrar vínculos históricos nem depender de produção. */
@EnabledIfEnvironmentVariable(named = "TURMALINA_MYSQL", matches = "true")
class ProductValueDeliveryMysql57Test {
  private final ObjectMapper json = new ObjectMapper();

  /** Aplica, reaplica e recusa colisão/fonte ausente preservando os contratos anteriores. */
  @Test
  void versionsTheProcessWithoutMigratingHistory() throws Exception {
    String host = System.getenv().getOrDefault("PDE_PRINCIPLES_DB_HOST", "127.0.0.1");
    assertThat(host).isIn("127.0.0.1", "sandbox-docker");
    try (Connection db =
        DriverManager.getConnection(
            "jdbc:mysql://"
                + host
                + ":33418/principles_test?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8",
            "root",
            "principles-local-only")) {
      assertThat(db.getMetaData().getDatabaseProductVersion()).startsWith("5.7.");
      for (String table :
          List.of(
              "agent_task",
              "product",
              "product_discovery_opportunity",
              "product_discovery_cycle",
              "business_process_chain_item",
              "business_process_chain_definition",
              "business_process_activity_definition",
              "business_process_definition",
              "DATABASECHANGELOG",
              "DATABASECHANGELOGLOCK")) execute(db, "DROP TABLE IF EXISTS " + table);
      for (String sql :
          Files.readString(Path.of("../../infra/testing/pde-commercial-principles/baseline.sql"))
              .split(";")) if (!sql.isBlank()) execute(db, sql);
      var graph =
          json.readTree(
              Path.of("../../infra/testing/turmalina-value-delivery/process-v11.json").toFile());
      try (var insert =
          db.prepareStatement(
              "INSERT INTO business_process_definition (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,process_type,execution_scope,diagram_json,created_at) VALUES ('pde-construction-approval','Construção','Valor','Backend','Plano aprovado','Produto completo',11,'PUBLISHED','PROCESS','PRODUCT',?,UTC_TIMESTAMP())")) {
        insert.setString(1, graph.toString());
        insert.executeUpdate();
      }
      String oldId = scalar(db, "SELECT id FROM business_process_definition");
      for (var node : graph.path("nodes"))
        if ("TASK".equals(node.path("type").asText())) {
          try (var insert =
              db.prepareStatement(
                  "INSERT INTO business_process_activity_definition (process_definition_id,activity_id,name,objective,owner_name,definition_json,created_at) VALUES (?, ?, ?, ?, ?, ?,UTC_TIMESTAMP())")) {
            insert.setString(1, oldId);
            insert.setString(2, node.path("id").asText());
            insert.setString(3, node.path("label").asText());
            insert.setString(4, node.path("description").asText());
            insert.setString(5, node.path("owner").asText());
            insert.setString(6, node.toString());
            insert.executeUpdate();
          }
        }
      execute(
          db,
          "INSERT INTO business_process_chain_definition (chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at) VALUES ('pde-value-creation-delivery','Cadeia','Valor','Entrega','Contribuição',27,'PUBLISHED',UTC_TIMESTAMP())");
      for (int n = 1; n <= 6; n++) {
        if (n != 3)
          execute(
              db,
              "INSERT INTO business_process_definition (process_code,name,purpose,owner_name,trigger_description,outcome_description,version_number,status,process_type,execution_scope,diagram_json,created_at) VALUES ('fixture-"
                  + n
                  + "','Etapa','Valor','Backend','Entrada','Saída',1,'PUBLISHED','PROCESS','PRODUCT','{}',UTC_TIMESTAMP())");
        execute(
            db,
            "INSERT INTO business_process_chain_item (chain_definition_id,process_definition_id,sequence_number,value_contribution,created_at) SELECT c.id,p.id,"
                + n
                + ",'Valor',UTC_TIMESTAMP() FROM business_process_chain_definition c JOIN business_process_definition p ON p.process_code='"
                + (n == 3 ? "pde-construction-approval" : "fixture-" + n)
                + "'");
      }
      execute(db, "INSERT INTO product SELECT 903,id FROM business_process_chain_definition");
      execute(
          db,
          "INSERT INTO agent_task VALUES (902,"
              + oldId
              + ",'experiment:903','COMPLETED',0.123,'{\"history\":true}')");
      String history =
          scalar(
              db,
              "SELECT CONCAT(process_definition_id,':',result_json,':',estimated_cost_usd) FROM agent_task");
      String binding = scalar(db, "SELECT chain_definition_id FROM product");
      var database =
          DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(db));
      try (var migration =
          new Liquibase(
              "db/changelog/changesets/2026-11-21-turmalina-value-delivery-v1.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {
        execute(db, "UPDATE business_process_definition SET status='RETIRED' WHERE id=" + oldId);
        assertThatThrownBy(() -> migration.update("")).hasMessageContaining("Precondition failed");
        assertThat(scalar(db, "SELECT COUNT(*) FROM business_process_chain_definition"))
            .isEqualTo("1");
        execute(db, "UPDATE business_process_definition SET status='PUBLISHED' WHERE id=" + oldId);
        execute(
            db,
            "INSERT INTO business_process_chain_definition (chain_code,name,purpose,outcome_description,primary_metric,version_number,status,created_at) VALUES ('pde-value-creation-delivery','Colisão','Valor','Entrega','Margem',28,'PUBLISHED',UTC_TIMESTAMP())");
        assertThatThrownBy(() -> migration.update("")).hasMessageContaining("Precondition failed");
        assertThat(
                scalar(
                    db, "SELECT COUNT(*) FROM business_process_definition WHERE version_number=12"))
            .isEqualTo("0");
        execute(db, "DELETE FROM business_process_chain_definition WHERE version_number=28");
        migration.update("");
        migration.update("");
        assertThat(scalar(db, "SELECT COUNT(*) FROM business_process_chain_definition"))
            .isEqualTo("2");
        assertThat(scalar(db, "SELECT COUNT(*) FROM business_process_activity_definition"))
            .isEqualTo("22");
        assertThat(
                scalar(
                    db,
                    "SELECT CONCAT(process_definition_id,':',result_json,':',estimated_cost_usd) FROM agent_task"))
            .isEqualTo(history);
        assertThat(scalar(db, "SELECT chain_definition_id FROM product")).isEqualTo(binding);
        assertThat(
                json.readTree(
                    scalar(
                        db,
                        "SELECT diagram_json FROM business_process_definition WHERE id=" + oldId)))
            .isEqualTo(graph);
        var updated =
            json.readTree(
                scalar(
                    db,
                    "SELECT diagram_json FROM business_process_definition WHERE process_code='pde-construction-approval' AND version_number=12"));
        assertThat(updated.path("flows")).isEqualTo(graph.path("flows"));
        assertThat(updated.path("productValueDeliveryContract").asText())
            .isEqualTo(ProductValueContract.VERSION);
        for (var node : updated.path("nodes"))
          if ("TASK".equals(node.path("type").asText())) {
            String objective =
                scalar(
                    db,
                    "SELECT a.objective FROM business_process_activity_definition a JOIN business_process_definition p ON p.id=a.process_definition_id WHERE p.version_number=12 AND a.activity_id='"
                        + node.path("id").asText()
                        + "'");
            assertThat(node.path("description").asText()).isEqualTo(objective);
          }
        assertThat(
                scalar(
                    db,
                    "SELECT COUNT(*) FROM business_process_chain_item i JOIN business_process_chain_definition c ON c.id=i.chain_definition_id WHERE c.version_number=28"))
            .isEqualTo("6");
        assertThatThrownBy(() -> migration.rollback(1, ""))
            .hasMessageContaining("Preserve execucoes");
        Path output = Path.of("target/turmalina");
        Files.createDirectories(output);
        var view =
            json.createObjectNode()
                .put(
                    "id",
                    Long.parseLong(
                        scalar(
                            db,
                            "SELECT id FROM business_process_definition WHERE process_code='pde-construction-approval' AND version_number=12")))
                .put("processCode", "pde-construction-approval")
                .put("name", "Protótipo, validação e aprovação por tipo")
                .put("versionNumber", 12)
                .put("status", "PUBLISHED")
                .put("ownerName", "Backend")
                .put("purpose", "Preservar proposta até a entrega")
                .put("outcomeDescription", "Produto completo comprovado")
                .put("processType", "PROCESS")
                .put("executionScope", "PRODUCT");
        view.set("diagram", updated);
        Files.writeString(
            output.resolve("processes.json"), json.createArrayNode().add(view).toPrettyString());
        var expected = json.createObjectNode();
        var entry = expected.putObject("pde-construction-approval").put("targetVersion", 12);
        var objectives = entry.putObject("objectives");
        for (var node : updated.path("nodes"))
          if ("TASK".equals(node.path("type").asText()))
            objectives.put(node.path("id").asText(), node.path("description").asText());
        Files.writeString(output.resolve("expected.json"), expected.toPrettyString());
      }
    }
  }

  /** Executa SQL somente na conexão efêmera da fixture. */
  private void execute(Connection db, String sql) throws Exception {
    try (var stmt = db.createStatement()) {
      stmt.execute(sql);
    }
  }

  /** Lê uma prova escalar de persistência para asserção do teste. */
  private String scalar(Connection db, String sql) throws Exception {
    try (var stmt = db.createStatement();
        var rows = stmt.executeQuery(sql)) {
      rows.next();
      return rows.getString(1);
    }
  }
}
