package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.WorkspaceResponse;
import com.marketinghub.pde.model.AccessGrant;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Controla liberação de acesso e progresso da cliente na experiência PDE. */
@Service
public class AccessService {
    private static final Logger log = LoggerFactory.getLogger(AccessService.class);
    private static final TypeReference<Map<String, StoredAccessGrant>> STORE_TYPE = new TypeReference<>() {};

    private final ProductCatalogService productCatalogService;
    private final ObjectMapper objectMapper;
    private final Path storagePath;
    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final Map<String, AccessGrant> accessByToken = new ConcurrentHashMap<>();

    /** Recebe dependências e carrega os acessos persistidos em disco. */
    @Autowired
    public AccessService(
            ProductCatalogService productCatalogService,
            ObjectMapper objectMapper,
            @Value("${pde.access.storage-path:/data/pde/access-grants.json}") String storagePath,
            @Value("${pde.access.jdbc-url:}") String jdbcUrl,
            @Value("${pde.access.jdbc-username:}") String jdbcUsername,
            @Value("${pde.access.jdbc-password:}") String jdbcPassword) {
        this.productCatalogService = productCatalogService;
        this.objectMapper = objectMapper;
        this.storagePath = Path.of(storagePath);
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
        loadPersistedAccess();
    }

    /** Recebe dependências para testes locais com persistência em arquivo. */
    public AccessService(ProductCatalogService productCatalogService, ObjectMapper objectMapper, String storagePath) {
        this(productCatalogService, objectMapper, storagePath, "", "", "");
    }

    /** Cria um acesso para um produto existente e retorna a URL da área da cliente. */
    public AccessResponse createAccess(String productSlug, String email, String source) {
        productCatalogService.getProduct(productSlug);
        AccessGrant existingGrant = findGrantByEmail(productSlug, email);
        if (existingGrant != null) {
            return toAccessResponse(existingGrant);
        }
        String token = UUID.randomUUID().toString();
        AccessGrant grant = new AccessGrant(token, productSlug, email, source, Instant.now());
        accessByToken.put(token, grant);
        persistAccess(grant);
        return toAccessResponse(grant);
    }

    /** Cadastra uma cliente do produto e retorna o acesso da Área MUSA. */
    public AccessResponse registerCustomer(String productSlug, String email) {
        return createAccess(productSlug, email, "CUSTOMER_REGISTRATION");
    }

    /** Recupera o acesso de uma cliente já cadastrada pelo e-mail informado. */
    public AccessResponse loginCustomer(String productSlug, String email) {
        productCatalogService.getProduct(productSlug);
        AccessGrant grant = findGrantByEmail(productSlug, email);
        if (grant == null) {
            throw new IllegalArgumentException("Cadastro da Area MUSA nao encontrado para este e-mail");
        }
        return toAccessResponse(grant);
    }

    /** Retorna a área de trabalho da cliente com produto e progresso atuais. */
    public WorkspaceResponse getWorkspace(String token) {
        AccessGrant grant = getGrant(token);
        ProductExperienceResponse product = productCatalogService.getProduct(grant.getProductSlug());
        Set<String> completedMissionIds = grant.getCompletedMissionIds();
        int totalMissions = product.missions().size();
        int completedMissions = completedMissionIds.size();
        int progressPercent = totalMissions == 0 ? 0 : Math.round((completedMissions * 100f) / totalMissions);
        return new WorkspaceResponse(
                product,
                grant.getEmail(),
                completedMissions,
                totalMissions,
                progressPercent,
                completedMissionIds.stream().toList());
    }

    /** Marca uma missão do produto como concluída após validar se ela existe. */
    public void completeMission(String token, String missionId) {
        AccessGrant grant = getGrant(token);
        ProductExperienceResponse product = productCatalogService.getProduct(grant.getProductSlug());
        boolean missionExists = product.missions().stream().anyMatch(mission -> mission.id().equals(missionId));
        if (!missionExists) {
            throw new IllegalArgumentException("Missao PDE nao encontrada: " + missionId);
        }
        grant.completeMission(missionId);
        persistAccess(grant);
    }

    /** Busca o acesso pelo token ou falha quando ele não existir. */
    private AccessGrant getGrant(String token) {
        AccessGrant grant = accessByToken.get(token);
        if (grant == null) {
            throw new IllegalArgumentException("Acesso PDE nao encontrado");
        }
        return grant;
    }

    /** Procura acesso já liberado para o mesmo produto e e-mail. */
    private AccessGrant findGrantByEmail(String productSlug, String email) {
        String normalizedEmail = normalizeEmail(email);
        return accessByToken.values().stream()
                .filter(grant -> grant.getProductSlug().equals(productSlug))
                .filter(grant -> normalizeEmail(grant.getEmail()).equals(normalizedEmail))
                .findFirst()
                .orElse(null);
    }

    /** Monta a resposta pública de acesso a partir do registro persistido. */
    private AccessResponse toAccessResponse(AccessGrant grant) {
        return new AccessResponse(
                grant.getToken(),
                grant.getProductSlug(),
                grant.getEmail(),
                grant.getSource(),
                "/access/" + grant.getToken());
    }

    /** Normaliza o e-mail para login e unicidade comercial. */
    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /** Carrega acessos persistidos para evitar perda de progresso em reinícios. */
    private void loadPersistedAccess() {
        if (usesJdbcStorage()) {
            loadPersistedAccessFromDatabase();
            return;
        }
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            Map<String, StoredAccessGrant> stored = objectMapper.readValue(storagePath.toFile(), STORE_TYPE);
            stored.forEach((token, value) -> accessByToken.put(token, value.toAccessGrant(token)));
        } catch (Exception ex) {
            log.error("Falha ao carregar acessos PDE persistidos em {}", storagePath, ex);
            throw new IllegalStateException("Nao foi possivel carregar acessos PDE persistidos", ex);
        }
    }

    /** Persiste os acessos e progresso no armazenamento configurado. */
    private synchronized void persistAccess(AccessGrant changedGrant) {
        if (usesJdbcStorage()) {
            persistAccessInDatabase(changedGrant);
            return;
        }
        persistAccessInFile();
    }

    /** Persiste os acessos e progresso em arquivo JSON local. */
    private synchronized void persistAccessInFile() {
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            Map<String, StoredAccessGrant> stored = new LinkedHashMap<>();
            accessByToken.forEach((token, grant) -> stored.put(token, StoredAccessGrant.from(grant)));
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), stored);
        } catch (IOException ex) {
            log.error("Falha ao persistir acessos PDE em {}", storagePath, ex);
            throw new IllegalStateException("Nao foi possivel persistir acesso PDE", ex);
        }
    }

    /** Informa se o backend PDE deve usar o banco MySQL do Marketing Hub. */
    private boolean usesJdbcStorage() {
        return jdbcUrl != null && !jdbcUrl.isBlank();
    }

    /** Abre conexão direta com o banco configurado para o PDE. */
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    /** Carrega acessos e missões concluídas a partir do MySQL do Marketing Hub. */
    private void loadPersistedAccessFromDatabase() {
        String sql = """
                SELECT g.token, g.product_slug, g.email, g.source, g.created_at, c.mission_id
                FROM pde_access_grant g
                LEFT JOIN pde_access_mission_completion c ON c.access_token = g.token
                ORDER BY g.created_at, c.completed_at
                """;
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            Map<String, StoredAccessGrantBuilder> builders = new LinkedHashMap<>();
            while (resultSet.next()) {
                String token = resultSet.getString("token");
                StoredAccessGrantBuilder builder = builders.get(token);
                if (builder == null) {
                    builder = new StoredAccessGrantBuilder(
                            resultSet.getString("product_slug"),
                            resultSet.getString("email"),
                            resultSet.getString("source"),
                            resultSet.getTimestamp("created_at").toInstant());
                    builders.put(token, builder);
                }
                String missionId = resultSet.getString("mission_id");
                if (missionId != null && !missionId.isBlank()) {
                    builder.completedMissionIds().add(missionId);
                }
            }
            builders.forEach((token, builder) -> accessByToken.put(token, builder.toAccessGrant(token)));
        } catch (SQLException ex) {
            log.error("Falha ao carregar acessos PDE no banco Marketing Hub", ex);
            throw new IllegalStateException("Nao foi possivel carregar acessos PDE no banco Marketing Hub", ex);
        }
    }

    /** Persiste o acesso alterado e suas missões concluídas no MySQL do Marketing Hub. */
    private void persistAccessInDatabase(AccessGrant grant) {
        String upsertGrant = """
                INSERT INTO pde_access_grant (token, product_slug, email, normalized_email, source, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                  email = VALUES(email),
                  source = VALUES(source),
                  updated_at = CURRENT_TIMESTAMP
                """;
        String insertMission = """
                INSERT IGNORE INTO pde_access_mission_completion (access_token, mission_id, completed_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                """;
        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(upsertGrant)) {
                statement.setString(1, grant.getToken());
                statement.setString(2, grant.getProductSlug());
                statement.setString(3, grant.getEmail());
                statement.setString(4, normalizeEmail(grant.getEmail()));
                statement.setString(5, grant.getSource());
                statement.setTimestamp(6, Timestamp.from(grant.getCreatedAt()));
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(insertMission)) {
                for (String missionId : grant.getCompletedMissionIds()) {
                    statement.setString(1, grant.getToken());
                    statement.setString(2, missionId);
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        } catch (SQLException ex) {
            log.error("Falha ao persistir acesso PDE no banco Marketing Hub; token={}", grant.getToken(), ex);
            throw new IllegalStateException("Nao foi possivel persistir acesso PDE no banco Marketing Hub", ex);
        }
    }

    /** Representa o formato persistido do acesso para armazenamento em JSON. */
    private record StoredAccessGrant(
            String productSlug,
            String email,
            String source,
            String createdAt,
            List<String> completedMissionIds) {

        /** Converte o acesso em memoria para o formato persistido. */
        private static StoredAccessGrant from(AccessGrant grant) {
            return new StoredAccessGrant(
                    grant.getProductSlug(),
                    grant.getEmail(),
                    grant.getSource(),
                    grant.getCreatedAt().toString(),
                    grant.getCompletedMissionIds().stream().toList());
        }

        /** Reconstrói o acesso de memoria a partir do JSON salvo. */
        private AccessGrant toAccessGrant(String token) {
            return new AccessGrant(
                    token,
                    productSlug,
                    email,
                    source,
                    Instant.parse(createdAt),
                    completedMissionIds != null ? Set.copyOf(completedMissionIds) : Set.of());
        }
    }

    /** Acumula dados lidos do banco antes de reconstruir o acesso em memória. */
    private record StoredAccessGrantBuilder(
            String productSlug,
            String email,
            String source,
            Instant createdAt,
            Set<String> completedMissionIds) {

        /** Cria o acumulador com conjunto mutável de missões concluídas. */
        private StoredAccessGrantBuilder(String productSlug, String email, String source, Instant createdAt) {
            this(productSlug, email, source, createdAt, ConcurrentHashMap.newKeySet());
        }

        /** Reconstrói o acesso em memória a partir das linhas do banco. */
        private AccessGrant toAccessGrant(String token) {
            return new AccessGrant(token, productSlug, email, source, createdAt, completedMissionIds);
        }
    }
}
