package com.marketinghub.pde.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Valida migrações idempotentes do schema operacional do PDE. */
class PdeDatabaseMigrationServiceTest {

    /** Confirma que a coluna e o índice de versão são criados quando faltam no banco PDE. */
    @Test
    void createsExperienceVersionColumnAndIndexWhenMissing() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableStatement = existingObjectStatement(true);
        PreparedStatement columnStatement = existingObjectStatement(false);
        PreparedStatement indexStatement = existingObjectStatement(false);
        PreparedStatement aiGuidanceTableStatement = existingObjectStatement(false);
        Statement ddlStatement = mock(Statement.class);
        when(connection.prepareStatement(anyString()))
                .thenReturn(tableStatement, columnStatement, indexStatement, aiGuidanceTableStatement);
        when(connection.createStatement()).thenReturn(ddlStatement);
        PdeDatabaseMigrationService migrationService = new PdeDatabaseMigrationService(
                "jdbc:mysql://pde", "user", "pass", (url, username, password) -> connection);

        migrationService.migrateIfNeeded();

        InOrder ddlOrder = inOrder(ddlStatement);
        ddlOrder.verify(ddlStatement).executeUpdate(
                "ALTER TABLE pde_funnel_event ADD COLUMN experience_version VARCHAR(80) NULL AFTER product_slug");
        ddlOrder.verify(ddlStatement).executeUpdate(
                "ALTER TABLE pde_funnel_event "
                        + "ADD KEY idx_pde_funnel_product_version_time "
                        + "(product_slug(100), experience_version(80), occurred_at)");
    }

    /** Confirma que a migração não executa DDL quando o schema já está atualizado. */
    @Test
    void skipsMigrationWhenExperienceVersionObjectsAlreadyExist() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableStatement = existingObjectStatement(true);
        PreparedStatement columnStatement = existingObjectStatement(true);
        PreparedStatement indexStatement = existingObjectStatement(true);
        PreparedStatement aiGuidanceTableStatement = existingObjectStatement(true);
        PreparedStatement aiGuidanceFkStatement = existingObjectStatement(false);
        PreparedStatement accessTokenLengthStatement = columnLengthStatement(120);
        when(connection.prepareStatement(anyString()))
                .thenReturn(
                        tableStatement,
                        columnStatement,
                        indexStatement,
                        aiGuidanceTableStatement,
                        aiGuidanceFkStatement,
                        accessTokenLengthStatement);
        PdeDatabaseMigrationService migrationService = new PdeDatabaseMigrationService(
                "jdbc:mysql://pde", "user", "pass", (url, username, password) -> connection);

        migrationService.migrateIfNeeded();

        verify(connection, never()).createStatement();
    }

    /** Confirma que token público maior é suportado quando o schema antigo tinha coluna curta. */
    @Test
    void expandsAiGuidanceAccessTokenWhenColumnIsTooShort() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableStatement = existingObjectStatement(true);
        PreparedStatement columnStatement = existingObjectStatement(true);
        PreparedStatement indexStatement = existingObjectStatement(true);
        PreparedStatement aiGuidanceTableStatement = existingObjectStatement(true);
        PreparedStatement aiGuidanceFkStatement = existingObjectStatement(true);
        PreparedStatement accessTokenLengthStatement = columnLengthStatement(40);
        Statement ddlStatement = mock(Statement.class);
        when(connection.prepareStatement(anyString()))
                .thenReturn(
                        tableStatement,
                        columnStatement,
                        indexStatement,
                        aiGuidanceTableStatement,
                        aiGuidanceFkStatement,
                        accessTokenLengthStatement);
        when(connection.createStatement()).thenReturn(ddlStatement);
        PdeDatabaseMigrationService migrationService = new PdeDatabaseMigrationService(
                "jdbc:mysql://pde", "user", "pass", (url, username, password) -> connection);

        migrationService.migrateIfNeeded();

        InOrder ddlOrder = inOrder(ddlStatement);
        ddlOrder.verify(ddlStatement)
                .executeUpdate("ALTER TABLE pde_ai_guidance_request DROP FOREIGN KEY fk_pde_ai_guidance_access_grant");
        ddlOrder.verify(ddlStatement)
                .executeUpdate("ALTER TABLE pde_ai_guidance_request MODIFY COLUMN access_token VARCHAR(120) NOT NULL");
    }

    /** Monta um statement de metadados que retorna existência ou ausência do objeto consultado. */
    private PreparedStatement existingObjectStatement(boolean exists) throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(exists ? 1L : 0L);
        return statement;
    }

    /** Monta um statement de metadados que retorna o tamanho máximo da coluna. */
    private PreparedStatement columnLengthStatement(int length) throws Exception {
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(length);
        when(resultSet.wasNull()).thenReturn(false);
        return statement;
    }
}
