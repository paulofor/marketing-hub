package com.marketinghub.mcpserver.service;

import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: expor diagnósticos de banco para o schema efetivo do PDE.
 */
@Service
public class PdeDatabaseDiagnosticsService extends DatabaseDiagnosticsService {

    private static final String NOT_CONFIGURED_MESSAGE = "PDE datasource is not configured";

    private final boolean configured;

    /**
     * Inicializa o serviço usando o JdbcTemplate dedicado do PDE quando estiver disponível.
     */
    public PdeDatabaseDiagnosticsService(@Qualifier("pdeJdbcTemplate") ObjectProvider<JdbcTemplate> pdeJdbcTemplate) {
        super(resolveJdbcTemplate(pdeJdbcTemplate));
        this.configured = pdeJdbcTemplate.getIfAvailable() != null;
    }

    /**
     * Informa se o datasource dedicado do PDE foi configurado no ambiente.
     */
    public boolean isConfigured() {
        return configured;
    }

    /**
     * Verifica a conexão do datasource PDE quando ele estiver configurado.
     */
    @Override
    public Map<String, Object> checkConnection() {
        ensureConfigured();
        return super.checkConnection();
    }

    /**
     * Lista tabelas do schema efetivo do PDE.
     */
    @Override
    public Map<String, Object> listTables() {
        ensureConfigured();
        return super.listTables();
    }

    /**
     * Lê tabela do schema efetivo do PDE com paginação.
     */
    @Override
    public Map<String, Object> readTable(String tableName, int limit, int offset) {
        ensureConfigured();
        return super.readTable(tableName, limit, offset);
    }

    /**
     * Executa SQL somente leitura no schema efetivo do PDE.
     */
    @Override
    public Map<String, Object> query(String sql, int limit) {
        ensureConfigured();
        return super.query(sql, limit);
    }

    /**
     * Bloqueia chamadas quando o datasource opcional do PDE não existe.
     */
    private void ensureConfigured() {
        if (!configured) {
            throw new IllegalStateException(NOT_CONFIGURED_MESSAGE);
        }
    }

    /**
     * Resolve o JdbcTemplate do PDE ou cria um stub que falha de forma explícita.
     */
    private static JdbcTemplate resolveJdbcTemplate(ObjectProvider<JdbcTemplate> pdeJdbcTemplate) {
        JdbcTemplate template = pdeJdbcTemplate.getIfAvailable();
        if (template != null) {
            return template;
        }
        return new JdbcTemplate() {
            @Override
            public <T> T queryForObject(String sql, Class<T> requiredType) {
                throw new IllegalStateException(NOT_CONFIGURED_MESSAGE);
            }
        };
    }
}
