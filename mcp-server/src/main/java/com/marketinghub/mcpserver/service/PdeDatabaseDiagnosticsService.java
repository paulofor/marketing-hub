package com.marketinghub.mcpserver.service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Responsabilidade: expor diagnósticos de banco para o schema efetivo do PDE.
 */
@Service
public class PdeDatabaseDiagnosticsService extends DatabaseDiagnosticsService {

    private static final String NOT_CONFIGURED_MESSAGE = "PDE datasource is not configured";

    private final boolean configured;
    private final String datasourceUrl;

    /**
     * Inicializa o serviço usando o JdbcTemplate dedicado do PDE quando estiver disponível.
     */
    public PdeDatabaseDiagnosticsService(
            @Qualifier("pdeJdbcTemplate") ObjectProvider<JdbcTemplate> pdeJdbcTemplate,
            Environment environment) {
        super(resolveJdbcTemplate(pdeJdbcTemplate));
        this.configured = pdeJdbcTemplate.getIfAvailable() != null;
        this.datasourceUrl = environment.getProperty("mcp.pde.datasource.url", "");
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
        Map<String, Object> response = new LinkedHashMap<>(super.checkConnection());
        response.put("datasourceTarget", describeDatasourceTarget(datasourceUrl));
        return response;
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

    /**
     * Descreve o alvo JDBC sem expor credenciais, para validar se o MCP consulta o banco produtivo certo.
     */
    private Map<String, Object> describeDatasourceTarget(String jdbcUrl) {
        Map<String, Object> target = new LinkedHashMap<>();
        String sanitizedUrl = sanitizeJdbcUrl(jdbcUrl);
        target.put("jdbcUrl", sanitizedUrl);
        try {
            URI uri = URI.create(jdbcUrl.replaceFirst("^jdbc:", ""));
            target.put("host", uri.getHost() == null ? "unknown" : uri.getHost());
            target.put("port", uri.getPort() == -1 ? 3306 : uri.getPort());
            target.put("schema", extractSchema(uri));
        } catch (RuntimeException ex) {
            target.put("host", "unknown");
            target.put("port", "unknown");
            target.put("schema", "unknown");
        }
        return target;
    }

    /**
     * Remove credenciais embutidas da URL JDBC antes de retornar a resposta MCP.
     */
    private String sanitizeJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            return "unknown";
        }
        return jdbcUrl.replaceAll("(?i)(jdbc:[^:]+://)([^:@/]+):([^@/]+)@", "$1$2:***@");
    }

    /**
     * Extrai o schema informado no path da URL JDBC.
     */
    private String extractSchema(URI uri) {
        String path = uri.getPath();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "unknown";
        }
        return path.substring(1);
    }
}
