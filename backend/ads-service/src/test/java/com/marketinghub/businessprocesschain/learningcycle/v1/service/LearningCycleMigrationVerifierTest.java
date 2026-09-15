package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import liquibase.ChecksumVersion;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.change.core.RawSQLChange;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir reversão posicional ou sem marco inequívoco na fixture dos ciclos. */
class LearningCycleMigrationVerifierTest {
  /** Mantém as migrações anteriores ao marco mesmo com novas migrações ao final da fixture. */
  @Test
  void rollsBackTargetAndLaterMigrationsOnly() throws Exception {
    var migration = migration("ciclo-v1", "bpm-v4", "automacao", "eventos", "incremento-futuro");
    LearningCycleMigrationVerifier.rollbackThrough(migration, "bpm-v4");
    verify(migration).rollback(eq(4), any(Contexts.class), any(LabelExpression.class));
  }

  /** Um marco que já é o último aplicado exige apenas a própria reversão. */
  @Test
  void rollsBackLatestMilestoneOnly() throws Exception {
    var migration = migration("ciclo-v1", "bpm-v4");
    LearningCycleMigrationVerifier.rollbackThrough(migration, "bpm-v4");
    verify(migration).rollback(eq(1), any(Contexts.class), any(LabelExpression.class));
  }

  /** A ausência do marco interrompe a validação antes de qualquer mudança no schema. */
  @Test
  void refusesMissingMilestone() throws Exception {
    var migration = migration("ciclo-v1");
    assertThrows(
        IllegalStateException.class,
        () -> LearningCycleMigrationVerifier.rollbackThrough(migration, "bpm-v4"));
    verify(migration, never()).rollback(anyInt(), any(Contexts.class), any(LabelExpression.class));
  }

  /** IDs repetidos em caminhos ou autores diferentes não autorizam escolher um arbitrariamente. */
  @Test
  void refusesAmbiguousMilestone() throws Exception {
    var migration = migration("ciclo-v1", "bpm-v4", "bpm-v4");
    assertThrows(
        IllegalStateException.class,
        () -> LearningCycleMigrationVerifier.rollbackThrough(migration, "bpm-v4"));
    verify(migration, never()).rollback(anyInt(), any(Contexts.class), any(LabelExpression.class));
  }

  /** Impede que um SQL sem inverso interrompa o rollback físico da fixture dos ciclos. */
  @Test
  void requiresExplicitRollbackForEveryRawSqlChangeInFixture() throws Exception {
    var missingRollbackIds =
        fixture().getChangeSets().stream()
            .filter(
                changeSet ->
                    changeSet.getChanges().stream().anyMatch(RawSQLChange.class::isInstance))
            .filter(changeSet -> !changeSet.hasCustomRollbackChanges())
            .map(changeSet -> changeSet.getId())
            .toList();

    org.junit.jupiter.api.Assertions.assertEquals(
        java.util.List.of(),
        missingRollbackIds,
        "Todo changeset SQL da fixture precisa declarar rollback explícito.");
  }

  /** Preserva o checksum da migração de Vega já aplicada antes de completar seu rollback. */
  @Test
  void preservesChecksumOfAppliedVegaMigration() throws Exception {
    var vega =
        fixture().getChangeSets().stream()
            .filter(changeSet -> "2026-09-11-vega-private-prototype-v1".equals(changeSet.getId()))
            .findFirst()
            .orElseThrow();

    org.junit.jupiter.api.Assertions.assertEquals(
        "9:31fe8d99dcb8790c102ee06a17f28258",
        vega.generateCheckSum(ChecksumVersion.latest()).toString(),
        "A migração já aplicada deve manter seu checksum histórico.");
  }

  /** Carrega a fixture real para validar contrato de rollback e compatibilidade histórica. */
  private DatabaseChangeLog fixture() throws Exception {
    var accessor = new ClassLoaderResourceAccessor();
    return ChangeLogParserFactory.getInstance()
        .getParser("learningcycle/changelog.yaml", accessor)
        .parse("learningcycle/changelog.yaml", new ChangeLogParameters(), accessor);
  }

  /** Monta o histórico ordenado que o Liquibase fornece, sem banco ou SQL produtivo. */
  private Liquibase migration(String... ids) throws Exception {
    var migration = mock(Liquibase.class);
    var database = mock(Database.class);
    when(migration.getDatabase()).thenReturn(database);
    when(database.getRanChangeSetList())
        .thenReturn(
            Arrays.stream(ids)
                .map(
                    id -> {
                      var change = new RanChangeSet();
                      change.setId(id);
                      return change;
                    })
                .toList());
    return migration;
  }
}
