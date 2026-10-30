package com.marketinghub.pde.kit.privateprototype.v1;

import static org.assertj.core.api.Assertions.*;

import com.marketinghub.pde.kit.privateprototype.v1.service.*;
import com.marketinghub.repository.jpa.kit.*;
import java.time.Instant;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Responsabilidade: comprovar persistência de 21 MiB e rollback no MySQL 5.7 com pacote de 16 MiB.
 */
@TestConfiguration
public class KitPrivateStorageLocalFixture {
  /**
   * Reproduz o limite antigo e valida bytes, isolamento, rollback e leitura após nova transação.
   */
  @Bean
  ApplicationRunner verifyStorage(
      DataSource source,
      PlatformTransactionManager manager,
      KitPrivateArtifactRepository artifacts,
      KitPrivateArtifactStorage storage,
      liquibase.integration.spring.SpringLiquibase liquibase) {
    return args -> {
      liquibase.afterPropertiesSet();
      var jdbc = new JdbcTemplate(source);
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID = '2026-10-30-private-kit-chunks-v1'",
                  Integer.class))
          .isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT @@max_allowed_packet", Long.class))
          .isEqualTo(16L * 1024 * 1024);
      byte[] bytes = new byte[21 * 1024 * 1024];
      new Random(3812).nextBytes(bytes);
      // A escrita monolítica reproduz o erro real, sem tocar em dados de produção.
      assertThatThrownBy(() -> jdbc.queryForObject("SELECT LENGTH(?)", Integer.class, bytes))
          .hasStackTraceContaining("Packet");
      var tx = new TransactionTemplate(manager);
      var first = artifact(7007L, 8007L, 9007L, bytes);
      tx.executeWithoutResult(
          status -> {
            artifacts.saveAndFlush(first);
            storage.store(first, bytes);
            artifacts.saveAndFlush(first);
          });
      tx.executeWithoutResult(
          status ->
              assertThat(storage.read(artifacts.findById(first.getId()).orElseThrow()))
                  .isEqualTo(bytes));
      var other = artifact(7018L, 8018L, 9018L, new byte[] {9, 8, 7});
      tx.executeWithoutResult(
          status -> {
            artifacts.saveAndFlush(other);
            storage.store(other, new byte[] {9, 8, 7});
            artifacts.saveAndFlush(other);
          });
      tx.executeWithoutResult(
          status ->
              assertThat(storage.read(artifacts.findById(other.getId()).orElseThrow()))
                  .containsExactly((byte) 9, (byte) 8, (byte) 7));
      var rollback = artifact(7007L, 8007L, 9007L, bytes);
      tx.executeWithoutResult(
          status -> {
            artifacts.saveAndFlush(rollback);
            storage.store(rollback, bytes);
            status.setRollbackOnly();
          });
      assertThat(
              jdbc.queryForObject(
                  "SELECT COUNT(*) FROM kit_private_artifact_chunk_v1 WHERE artifact_id = ?",
                  Integer.class,
                  rollback.getId()))
          .isZero();
      assertThat(artifacts.findById(rollback.getId())).isEmpty();
      assertThat(
              jdbc.queryForObject(
                  "SELECT MAX(OCTET_LENGTH(part_bytes)) FROM kit_private_artifact_chunk_v1",
                  Integer.class))
          .isEqualTo(KitPrivateArtifactStorage.PART_BYTES);
      // Remove apenas fixtures de armazenamento, sem consumir as reservas dos cenários de
      // navegador.
      jdbc.update("DELETE FROM kit_private_artifact_chunk_v1 WHERE artifact_id = ?", first.getId());
      jdbc.update(
          "DELETE FROM kit_private_artifact_v1 WHERE id IN (?,?)", first.getId(), other.getId());
      System.out.println(
          "PRIVATE_KIT_STORAGE_OK packet=16777216 bytes=22020096 rollback=true isolation=true legacy=true");
    };
  }

  /** Cria metadados segregados e sintéticos para o teste físico do armazenamento. */
  private KitPrivateArtifact artifact(long cycle, long product, long experiment, byte[] bytes) {
    var a = new KitPrivateArtifact();
    a.setId(UUID.randomUUID().toString());
    a.setCycleId(cycle);
    a.setProductId(product);
    a.setExperimentId(experiment);
    a.setPrototypeVersion("storage-test");
    a.setProfileCode("barber-v1");
    a.setFixtureOwner("STORAGE_TEST");
    a.setInputSha256(
        KitArtifactContract.hash(a.getId().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    a.setInputJson("{}");
    a.setStatus("READY");
    a.setCreatedAt(Instant.now());
    a.setZipSha256(KitArtifactContract.hash(bytes));
    return a;
  }
}
