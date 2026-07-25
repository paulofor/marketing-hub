package com.marketinghub.pde.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Valida o diagnóstico operacional usado antes de interpretar métricas PDE. */
class PdeOperationalHealthServiceTest {

    /** Confirma que o status aprova os campos necessários para métricas de dispositivo e tela. */
    @Test
    void reportsFunnelAnalyticsFieldsReadyWhenSchemaContainsTrackingColumns() throws Exception {
        Connection connection = mock(Connection.class);
        List<PreparedStatement> statements = new ArrayList<>();
        for (int index = 0; index < 30; index++) {
            statements.add(existingObjectStatement(true));
        }
        statements.add(existingObjectStatement(false));
        statements.add(existingObjectStatement(false));
        when(connection.prepareStatement(anyString())).thenReturn(
                statements.get(0),
                statements.subList(1, statements.size()).toArray(PreparedStatement[]::new));
        PdeOperationalHealthService service = new PdeOperationalHealthService(
                "jdbc:mysql://pde", "user", "pass", (url, username, password) -> connection);

        var status = service.schemaStatus();

        assertThat(status.jdbcConfigured()).isTrue();
        assertThat(status.funnelEventTableExists()).isTrue();
        assertThat(status.funnelAnalyticsFieldsReady()).isTrue();
    }

    /** Confirma que o status bloqueia leitura comercial quando faltam campos de analytics. */
    @Test
    void reportsFunnelAnalyticsFieldsNotReadyWhenSchemaIsLegacy() throws Exception {
        Connection schemaConnection = mock(Connection.class);
        Connection alertConnection = mock(Connection.class);
        PreparedStatement schemaFunnelTableStatement = existingObjectStatement(true);
        PreparedStatement schemaFirstAnalyticsColumnStatement = existingObjectStatement(false);
        PreparedStatement schemaAiGuidanceTableStatement = existingObjectStatement(false);
        PreparedStatement schemaFailureTableStatement = existingObjectStatement(false);
        PreparedStatement alertFunnelTableStatement = existingObjectStatement(true);
        PreparedStatement alertFirstAnalyticsColumnStatement = existingObjectStatement(false);
        PreparedStatement alertAiGuidanceTableStatement = existingObjectStatement(false);
        PreparedStatement alertFailureTableStatement = existingObjectStatement(false);
        when(schemaConnection.prepareStatement(anyString()))
                .thenReturn(
                        schemaFunnelTableStatement,
                        schemaFirstAnalyticsColumnStatement,
                        schemaAiGuidanceTableStatement,
                        schemaFailureTableStatement);
        when(alertConnection.prepareStatement(anyString()))
                .thenReturn(
                        alertFunnelTableStatement,
                        alertFirstAnalyticsColumnStatement,
                        alertAiGuidanceTableStatement,
                        alertFailureTableStatement);
        PdeOperationalHealthService service = new PdeOperationalHealthService(
                "jdbc:mysql://pde",
                "user",
                "pass",
                new PdeOperationalHealthService.JdbcConnectionProvider() {
                    private int calls;

                    /** Retorna conexões separadas para schemaStatus e operationalAlerts. */
                    @Override
                    public Connection open(String url, String username, String password) {
                        calls += 1;
                        return calls == 1 ? schemaConnection : alertConnection;
                    }
                });

        var status = service.schemaStatus();

        assertThat(status.jdbcConfigured()).isTrue();
        assertThat(status.funnelEventTableExists()).isTrue();
        assertThat(status.funnelAnalyticsFieldsReady()).isFalse();
        assertThat(service.operationalAlerts())
                .anySatisfy(alert -> {
                    assertThat(alert.type()).isEqualTo("SCHEMA_MISMATCH");
                    assertThat(alert.funnelStage()).isEqualTo("TRACKING_FUNIL");
                });
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
}
