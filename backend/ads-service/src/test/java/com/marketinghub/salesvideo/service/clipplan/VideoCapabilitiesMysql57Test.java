package com.marketinghub.salesvideo.service.clipplan;

import static org.assertj.core.api.Assertions.*;

import java.sql.DriverManager;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Responsabilidade: aplicar a migração real em MySQL 5.7 efêmero, preservando Pippit e gates
 * antigos.
 */
@EnabledIfSystemProperty(named = "studio.mysql", matches = "true")
class VideoCapabilitiesMysql57Test {
  /**
   * Valida SQL real, bootstrap das novas colunas, replay Liquibase e histórico em outra plataforma.
   */
  @Test
  void migratesWithoutRetargetingPippitOrActivatingModels() throws Exception {
    try (var connection =
        DriverManager.getConnection(
            System.getProperty(
                "studio.mysql.url",
                "jdbc:mysql://sandbox-docker:33317/studio_test?useSSL=false&allowPublicKeyRetrieval=true"),
            "root",
            "studio_test_only")) {
      try (var sql = connection.createStatement()) {
        sql.execute("DROP TABLE IF EXISTS DATABASECHANGELOGLOCK");
        sql.execute("DROP TABLE IF EXISTS DATABASECHANGELOG");
        sql.execute("DROP TABLE IF EXISTS sales_video_provider_model");
        sql.execute("DROP TABLE IF EXISTS video_project");
        sql.execute("DROP TABLE IF EXISTS video_credit_reservation");
        sql.execute("CREATE TABLE video_project (id BIGINT PRIMARY KEY)");
        sql.execute("CREATE TABLE video_credit_reservation (id BIGINT PRIMARY KEY)");
        sql.execute(
            """
          CREATE TABLE sales_video_provider_model (
            id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,code VARCHAR(80) NOT NULL UNIQUE,
            display_name VARCHAR(140) NOT NULL,manufacturer_name VARCHAR(120) NOT NULL,
            aggregator_name VARCHAR(120) NOT NULL,provider_account_key VARCHAR(80),route_key VARCHAR(120) NOT NULL,
            provider_name VARCHAR(80) NOT NULL UNIQUE,provider_family VARCHAR(40) NOT NULL,adapter_key VARCHAR(80) NOT NULL,
            external_model_id VARCHAR(120) NOT NULL,recommended_use VARCHAR(500) NOT NULL,lifecycle_status VARCHAR(24) NOT NULL,
            clip_duration_seconds INT NOT NULL,max_direct_duration_seconds INT NOT NULL,
            supports_hero_video TINYINT(1) NOT NULL,supports_scene_assembly TINYINT(1) NOT NULL,requires_source_image TINYINT(1) NOT NULL,
            credits_url VARCHAR(500),documentation_url VARCHAR(500) NOT NULL,
            adapter_verified TINYINT(1) NOT NULL,pricing_verified TINYINT(1) NOT NULL,commercial_license_verified TINYINT(1) NOT NULL,
            quality_gate_verified TINYINT(1) NOT NULL,notes VARCHAR(1000),created_at DATETIME NOT NULL,updated_at DATETIME NOT NULL,
            pricing_research_status VARCHAR(24) NOT NULL DEFAULT 'PENDING') ENGINE=InnoDB
          """);
        sql.execute(
            """
          INSERT INTO sales_video_provider_model
          (id,code,display_name,manufacturer_name,aggregator_name,provider_account_key,route_key,provider_name,provider_family,adapter_key,external_model_id,
           recommended_use,lifecycle_status,clip_duration_seconds,max_direct_duration_seconds,supports_hero_video,supports_scene_assembly,requires_source_image,
           credits_url,documentation_url,adapter_verified,pricing_verified,commercial_license_verified,quality_gate_verified,notes,created_at,updated_at)
          VALUES(13,'pippit-seedance-2-5','Seedance antigo','ByteDance','Pippit',NULL,'old-route','PIPPIT_SEEDANCE_2_5','EXTERNAL_VIDEO_MODULE','PIPPIT','seedance-2.5',
           'Histórico preservado','DRAFT',30,30,0,0,0,NULL,'https://example.invalid/history',0,0,0,0,'Prova histórica imutável',NOW(),NOW())
          """);
      }
      var database =
          DatabaseFactory.getInstance()
              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      try (var liquibase =
          new Liquibase(
              "db/changelog/changesets/2026-11-05-video-capabilities-v1.yaml",
              new ClassLoaderResourceAccessor(),
              database)) {
        liquibase.update(new Contexts(), new LabelExpression());
        liquibase.update(new Contexts(), new LabelExpression());
        try (var sql = connection.createStatement()) {
          try (var rows =
              sql.executeQuery(
                  "SELECT aggregator_name,adapter_key,route_key,notes,lifecycle_status FROM sales_video_provider_model WHERE id=13")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getString(1)).isEqualTo("Pippit");
            assertThat(rows.getString(2)).isEqualTo("PIPPIT");
            assertThat(rows.getString(3)).isEqualTo("old-route");
            assertThat(rows.getString(4)).isEqualTo("Prova histórica imutável");
            assertThat(rows.getString(5)).isEqualTo("DRAFT");
          }
          try (var rows =
              sql.executeQuery(
                  "SELECT COUNT(*),SUM(adapter_verified+pricing_verified+commercial_license_verified+quality_gate_verified) FROM sales_video_provider_model WHERE adapter_key='RUNWAY'")) {
            rows.next();
            assertThat(rows.getInt(1)).isEqualTo(6);
            assertThat(rows.getInt(2)).isZero();
          }
          try (var rows =
              sql.executeQuery(
                  "SELECT external_model_id,max_direct_duration_seconds,provider_account_key FROM sales_video_provider_model WHERE provider_name='RUNWAY_SEEDANCE_2_5'")) {
            rows.next();
            assertThat(rows.getString(1)).isEqualTo("seedance2_5");
            assertThat(rows.getInt(2)).isEqualTo(30);
            assertThat(rows.getString(3)).isEqualTo("RUNWAY_PRIMARY");
          }
          sql.execute("SELECT production_cost_plan_json FROM video_project");
          sql.execute("SELECT full_production_reserved_usd FROM video_credit_reservation");
        }
      }
    }
  }
}
