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
        PreparedStatement operationalFailureTableStatement = existingObjectStatement(true);
        Statement ddlStatement = mock(Statement.class);
        when(connection.prepareStatement(anyString()))
                .thenReturn(
                        tableStatement,
                        columnStatement,
                        indexStatement,
                        aiGuidanceTableStatement,
                        operationalFailureTableStatement);
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
        PreparedStatement operationalFailureTableStatement = existingObjectStatement(true);
        when(connection.prepareStatement(anyString()))
                .thenReturn(
                        tableStatement,
                        columnStatement,
                        indexStatement,
                        aiGuidanceTableStatement,
                        aiGuidanceFkStatement,
                        accessTokenLengthStatement,
                        operationalFailureTableStatement);
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
        PreparedStatement operationalFailureTableStatement = existingObjectStatement(true);
        Statement ddlStatement = mock(Statement.class);
        when(connection.prepareStatement(anyString()))
                .thenReturn(
                        tableStatement,
                        columnStatement,
                        indexStatement,
                        aiGuidanceTableStatement,
                        aiGuidanceFkStatement,
                        accessTokenLengthStatement,
                        operationalFailureTableStatement);
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

    /** Confirma que a migração de startup prepara o diagnóstico público sem depender do fluxo de login. */
    @Test
    void migratesAiGuidancePublicAccessOnStartup() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableStatement = existingObjectStatement(true);
        PreparedStatement columnStatement = existingObjectStatement(true);
        PreparedStatement indexStatement = existingObjectStatement(true);
        PreparedStatement aiGuidanceTableStatement = existingObjectStatement(true);
        PreparedStatement aiGuidanceFkStatement = existingObjectStatement(true);
        PreparedStatement accessTokenLengthStatement = columnLengthStatement(36);
        PreparedStatement operationalFailureTableStatement = existingObjectStatement(true);
        Statement ddlStatement = mock(Statement.class);
        when(connection.prepareStatement(anyString()))
                .thenReturn(
                        tableStatement,
                        columnStatement,
                        indexStatement,
                        aiGuidanceTableStatement,
                        aiGuidanceFkStatement,
                        accessTokenLengthStatement,
                        operationalFailureTableStatement);
        when(connection.createStatement()).thenReturn(ddlStatement);
        PdeDatabaseMigrationService migrationService = new PdeDatabaseMigrationService(
                "jdbc:mysql://pde", "user", "pass", (url, username, password) -> connection);

        migrationService.migrateOnStartup();

        InOrder ddlOrder = inOrder(ddlStatement);
        ddlOrder.verify(ddlStatement)
                .executeUpdate("ALTER TABLE pde_ai_guidance_request DROP FOREIGN KEY fk_pde_ai_guidance_access_grant");
        ddlOrder.verify(ddlStatement)
                .executeUpdate("ALTER TABLE pde_ai_guidance_request MODIFY COLUMN access_token VARCHAR(120) NOT NULL");
    }

    /** Confirma que a tabela de falhas operacionais é criada para alertar o pós-deploy. */
    @Test
    void createsOperationalFailureTableWhenMissing() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableStatement = existingObjectStatement(true);
        PreparedStatement columnStatement = existingObjectStatement(true);
        PreparedStatement indexStatement = existingObjectStatement(true);
        PreparedStatement aiGuidanceTableStatement = existingObjectStatement(true);
        PreparedStatement aiGuidanceFkStatement = existingObjectStatement(false);
        PreparedStatement accessTokenLengthStatement = columnLengthStatement(120);
        PreparedStatement operationalFailureTableStatement = existingObjectStatement(false);
        Statement ddlStatement = mock(Statement.class);
        when(connection.prepareStatement(anyString()))
                .thenReturn(
                        tableStatement,
                        columnStatement,
                        indexStatement,
                        aiGuidanceTableStatement,
                        aiGuidanceFkStatement,
                        accessTokenLengthStatement,
                        operationalFailureTableStatement);
        when(connection.createStatement()).thenReturn(ddlStatement);
        PdeDatabaseMigrationService migrationService = new PdeDatabaseMigrationService(
                "jdbc:mysql://pde", "user", "pass", (url, username, password) -> connection);

        migrationService.migrateIfNeeded();

        verify(ddlStatement).executeUpdate(org.mockito.ArgumentMatchers.contains(
                "CREATE TABLE pde_operational_endpoint_failure"));
    }

    /** Confirma que a tabela de orientação IA é criada quando o schema antigo não a possui. */
    @Test
    void createsAiGuidanceTableWhenMissing() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement tableStatement = existingObjectStatement(true);
        PreparedStatement columnStatement = existingObjectStatement(true);
        PreparedStatement indexStatement = existingObjectStatement(true);
        PreparedStatement aiGuidanceTableStatement = existingObjectStatement(false);
        PreparedStatement aiGuidanceFkStatement = existingObjectStatement(false);
        PreparedStatement accessTokenLengthStatement = columnLengthStatement(120);
        PreparedStatement operationalFailureTableStatement = existingObjectStatement(true);
        Statement ddlStatement = mock(Statement.class);
        when(connection.prepareStatement(anyString()))
                .thenReturn(
                        tableStatement,
                        columnStatement,
                        indexStatement,
                        aiGuidanceTableStatement,
                        aiGuidanceFkStatement,
                        accessTokenLengthStatement,
                        operationalFailureTableStatement);
        when(connection.createStatement()).thenReturn(ddlStatement);
        PdeDatabaseMigrationService migrationService = new PdeDatabaseMigrationService(
                "jdbc:mysql://pde", "user", "pass", (url, username, password) -> connection);

        migrationService.migrateIfNeeded();

        verify(ddlStatement).executeUpdate(org.mockito.ArgumentMatchers.contains(
                "CREATE TABLE pde_ai_guidance_request"));
    }

    /** Confirma que a checagem sob demanda repara token curto mesmo após migração inicial. */
    @Test
    void ensuresAiGuidanceStorageReadyAfterStartupMigrationAlreadyRan() throws Exception {
        Connection startupConnection = mock(Connection.class);
        Connection runtimeConnection = mock(Connection.class);
        PreparedStatement funnelTableStatement = existingObjectStatement(false);
        PreparedStatement startupAiGuidanceTableStatement = existingObjectStatement(true);
        PreparedStatement startupAiGuidanceFkStatement = existingObjectStatement(false);
        PreparedStatement startupAccessTokenLengthStatement = columnLengthStatement(120);
        PreparedStatement operationalFailureTableStatement = existingObjectStatement(true);
        PreparedStatement runtimeAiGuidanceTableStatement = existingObjectStatement(true);
        PreparedStatement runtimeAiGuidanceFkStatement = existingObjectStatement(false);
        PreparedStatement runtimeAccessTokenLengthStatement = columnLengthStatement(40);
        Statement runtimeDdlStatement = mock(Statement.class);
        when(startupConnection.prepareStatement(anyString()))
                .thenReturn(
                        funnelTableStatement,
                        startupAiGuidanceTableStatement,
                        startupAiGuidanceFkStatement,
                        startupAccessTokenLengthStatement,
                        operationalFailureTableStatement);
        when(runtimeConnection.prepareStatement(anyString()))
                .thenReturn(
                        runtimeAiGuidanceTableStatement,
                        runtimeAiGuidanceFkStatement,
                        runtimeAccessTokenLengthStatement);
        when(runtimeConnection.createStatement()).thenReturn(runtimeDdlStatement);
        PdeDatabaseMigrationService migrationService = new PdeDatabaseMigrationService(
                "jdbc:mysql://pde",
                "user",
                "pass",
                new PdeDatabaseMigrationService.JdbcConnectionProvider() {
                    private int calls;

                    /** Retorna conexões diferentes para simular startup e execução em produção. */
                    @Override
                    public Connection open(String url, String username, String password) {
                        calls += 1;
                        return calls == 1 ? startupConnection : runtimeConnection;
                    }
                });

        migrationService.migrateIfNeeded();
        migrationService.ensureAiGuidanceStorageReady();

        verify(runtimeDdlStatement)
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
