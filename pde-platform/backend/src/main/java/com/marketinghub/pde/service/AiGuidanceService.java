package com.marketinghub.pde.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.dto.AiGuidanceCreateRequest;
import com.marketinghub.pde.dto.AiGuidancePendingResponse;
import com.marketinghub.pde.dto.AiGuidanceResponse;
import com.marketinghub.pde.dto.AiGuidanceResultRequest;
import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.MissionInteractionRequest;
import com.marketinghub.pde.dto.ProductExperienceResponse;
import com.marketinghub.pde.dto.PublicPresenceDiagnosticRequest;
import com.marketinghub.pde.dto.WorkspaceResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Controla solicitações de orientação por IA do PDE sem executar OpenAI no backend. */
@Service
public class AiGuidanceService {
    private static final Logger log = LoggerFactory.getLogger(AiGuidanceService.class);
    private static final TypeReference<Map<String, StoredAiGuidance>> STORE_TYPE = new TypeReference<>() {};
    private static final String STAGE_CODE = "pde-ai-guidance-v1";
    private static final String MUSA_PRODUCT_SLUG = "metodo-musa-7-dias";
    private static final String PUBLIC_ACCESS_PREFIX = "public-presence-diagnostic:";
    private static final String PUBLIC_DIAGNOSTIC_MISSION_ID = "diagnostico-presenca-publico";
    private static final String PUBLIC_DIAGNOSTIC_GUIDANCE_TYPE = "MUSA_PUBLIC_PRESENCE_DIAGNOSTIC";
    private static final String MUSA_V7_EXPERIENCE_VERSION = "musa-pde-entry-v7-espelho-antes-de-sair";
    private static final String LOCAL_RULES_MODEL = "MUSA_LOCAL_RULES_V1";
    private static final String MUSA_NEUTRAL_CHOICE = "Manter como está por enquanto";
    private static final Set<String> ALLOWED_GUIDANCE_TYPES = Set.of(
            PUBLIC_DIAGNOSTIC_GUIDANCE_TYPE,
            "MUSA_DAY_1_PRESENCE_DIAGNOSIS",
            "MUSA_DAY_2_SIGNATURE",
            "MUSA_DAY_3_WARDROBE_REUSE",
            "MUSA_DAY_4_FINISHING_RITUAL",
            "MUSA_DAY_5_ANTI_IMPULSE_DECISION",
            "MUSA_DAY_6_OCCASION_ENTRY",
            "MUSA_DAY_7_MAINTENANCE_PLAN");

    private final AccessService accessService;
    private final ProductCatalogService productCatalogService;
    private final ObjectMapper objectMapper;
    private final Path storagePath;
    private final String jdbcUrl;
    private final String jdbcUsername;
    private final String jdbcPassword;
    private final PdeDatabaseMigrationService databaseMigrationService;
    private final Map<String, StoredAiGuidance> requestsById = new ConcurrentHashMap<>();

    /** Recebe dependências e carrega solicitações de IA já persistidas. */
    public AiGuidanceService(
            AccessService accessService,
            ProductCatalogService productCatalogService,
            ObjectMapper objectMapper,
            @Value("${pde.ai.storage-path:/data/pde/ai-guidance-requests.json}") String storagePath,
            @Value("${pde.access.jdbc-url:}") String jdbcUrl,
            @Value("${pde.access.jdbc-username:}") String jdbcUsername,
            @Value("${pde.access.jdbc-password:}") String jdbcPassword,
            PdeDatabaseMigrationService databaseMigrationService) {
        this.accessService = accessService;
        this.productCatalogService = productCatalogService;
        this.objectMapper = objectMapper;
        this.storagePath = Path.of(storagePath);
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
        this.databaseMigrationService = databaseMigrationService;
        loadPersistedRequests();
    }

    /** Cria uma solicitação de orientação por IA e salva as respostas da missão. */
    public AiGuidanceResponse createGuidanceRequest(String token, String missionId, AiGuidanceCreateRequest request) {
        validateGuidanceType(request.guidanceType());
        WorkspaceResponse workspace = accessService.getWorkspace(token);
        validateMissionBelongsToWorkspace(workspace, missionId);
        boolean useLocalRules = usesMusaV7LocalRules(
                request.experienceVersion(), workspace.product().experienceVersion());
        if (useLocalRules) {
            validateMusaV7CategoricalAnswers(request.answers());
        }
        accessService.saveMissionInteraction(token, missionId, new MissionInteractionRequest(request.answers()));
        workspace = accessService.getWorkspace(token);
        String requestId = UUID.randomUUID().toString();
        StoredAiGuidance stored = StoredAiGuidance.pending(
                requestId,
                token,
                workspace.product().slug(),
                workspace.email(),
                missionId,
                request.guidanceType(),
                sanitizeAnswers(request.answers()),
                previousMissionAnswers(workspace, missionId),
                Instant.now().toString());
        if (useLocalRules) {
            stored = completeWithLocalRules(stored);
        }
        requestsById.put(requestId, stored);
        persistRequest(stored);
        accessService.recordFunnelEvent(new FunnelEventRequest(
                workspace.product().slug(),
                "AI_GUIDANCE_REQUESTED",
                token,
                workspace.email(),
                workspace.accessSource(),
                "pde-platform",
                null,
                Map.of("missionId", missionId, "guidanceType", request.guidanceType(), "requestId", requestId)));
        return toResponse(stored);
    }

    /** Cria um diagnóstico público de presença sem exigir e-mail antes da entrega. */
    public AiGuidanceResponse createPublicPresenceDiagnostic(PublicPresenceDiagnosticRequest request) {
        validateGuidanceType(PUBLIC_DIAGNOSTIC_GUIDANCE_TYPE);
        String requestId = UUID.randomUUID().toString();
        StoredAiGuidance stored = StoredAiGuidance.pending(
                requestId,
                PUBLIC_ACCESS_PREFIX + requestId,
                MUSA_PRODUCT_SLUG,
                "diagnostico-publico@musa.local",
                PUBLIC_DIAGNOSTIC_MISSION_ID,
                PUBLIC_DIAGNOSTIC_GUIDANCE_TYPE,
                sanitizeAnswers(request.answers()),
                Map.of(),
                Instant.now().toString());
        if (usesMusaV7LocalRules(request.experienceVersion(), null)) {
            stored = completeWithLocalRules(stored);
        }
        requestsById.put(requestId, stored);
        persistRequest(stored);
        return toResponse(stored);
    }

    /** Retorna o diagnóstico público de presença pelo identificador opaco da solicitação. */
    public AiGuidanceResponse getPublicPresenceDiagnostic(String requestId) {
        StoredAiGuidance stored = getRequest(requestId);
        if (!stored.accessToken().startsWith(PUBLIC_ACCESS_PREFIX)) {
            throw new IllegalArgumentException("Diagnóstico público de presença não encontrado");
        }
        return toResponse(stored);
    }

    /** Retorna uma orientação da cliente, concluída ou ainda pendente. */
    public AiGuidanceResponse getGuidance(String token, String requestId) {
        StoredAiGuidance stored = getRequest(requestId);
        if (!stored.accessToken().equals(token)) {
            throw new IllegalArgumentException("Orientação PDE não encontrada para este acesso");
        }
        return toResponse(stored);
    }

    /** Entrega a próxima solicitação pendente ao worker executor do PDE. */
    public Optional<AiGuidancePendingResponse> getPendingGuidance() {
        Optional<StoredAiGuidance> pending = usesJdbcStorage()
                ? loadPendingGuidanceFromDatabase()
                : requestsById.values().stream()
                        .filter(request -> "PENDING".equals(request.status()))
                        .sorted((left, right) -> left.createdAt().compareTo(right.createdAt()))
                        .findFirst();
        return pending.map(this::toPendingResponse);
    }

    /** Recebe do worker o resultado funcional e os dados de auditoria da chamada OpenAI. */
    public AiGuidanceResponse receiveGuidanceResult(String requestId, AiGuidanceResultRequest result) {
        StoredAiGuidance current = getRequest(requestId);
        String normalizedStatus = normalizeResultStatus(result.status());
        StoredAiGuidance updated = current.withResult(
                normalizedStatus,
                nullToBlank(result.headline()),
                nullToBlank(result.summary()),
                result.signals() == null ? List.of() : result.signals(),
                result.microActions() == null ? List.of() : result.microActions(),
                nullToBlank(result.caution()),
                nullToBlank(result.model()),
                nullToBlank(result.serviceTier()),
                nullToBlank(result.rawRequestJson()),
                nullToBlank(result.rawResponseJson()),
                result.inputTokens(),
                result.outputTokens(),
                result.costUsd(),
                nullToBlank(result.errorMessage()),
                Instant.now().toString());
        requestsById.put(requestId, updated);
        persistRequest(updated);
        return toResponse(updated);
    }

    /** Confirma que o tipo de orientação está no contrato fechado do produto. */
    private void validateGuidanceType(String guidanceType) {
        if (!ALLOWED_GUIDANCE_TYPES.contains(guidanceType)) {
            throw new IllegalArgumentException("Tipo de orientação PDE não suportado: " + guidanceType);
        }
    }

    /** Confirma que a missão solicitada pertence ao produto da cliente. */
    private void validateMissionBelongsToWorkspace(WorkspaceResponse workspace, String missionId) {
        boolean exists = workspace.product().missions().stream().anyMatch(mission -> mission.id().equals(missionId));
        if (!exists) {
            throw new IllegalArgumentException("Missão PDE não encontrada: " + missionId);
        }
    }

    /** Confirma quando a versão aprovada exige regras locais e proíbe envio ao worker de IA. */
    private boolean usesMusaV7LocalRules(String requestedVersion, String workspaceVersion) {
        return MUSA_V7_EXPERIENCE_VERSION.equals(nullToBlank(requestedVersion))
                || MUSA_V7_EXPERIENCE_VERSION.equals(nullToBlank(workspaceVersion));
    }

    /** Conclui a orientação por regras determinísticas, sem fila, tokens ou chamada externa. */
    private StoredAiGuidance completeWithLocalRules(StoredAiGuidance pending) {
        validateMusaV7CategoricalAnswers(pending.answers());
        boolean neutralPath = pending.answers().values().stream()
                .anyMatch(value -> MUSA_NEUTRAL_CHOICE.equals(value)
                        || "Minha imagem está coerente; quero apenas organizar minhas escolhas".equals(value));
        List<String> selectedSignals = pending.answers().values().stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .limit(3)
                .toList();
        String context = pending.answers().getOrDefault(
                "presenceFocus", pending.answers().getOrDefault("realOccasion", "uma situação real"));
        String desired = pending.answers().getOrDefault(
                "desiredSignal", pending.answers().getOrDefault("bestSignal", "mais intenção"));
        String resource = pending.answers().getOrDefault(
                "startingResource", pending.answers().getOrDefault("pieces", "algo que você já possui"));
        List<String> actions = neutralPath
                ? List.of(
                        "Mantenha sua escolha atual sem correção obrigatória.",
                        "Se quiser, apenas observe o que já funciona para você.",
                        "Você pode seguir para o próximo dia sem realizar uma microação.")
                : List.of(
                        "Prepare " + resource.toLowerCase() + " para " + context.toLowerCase() + ".",
                        "Escolha somente um detalhe observável para reforçar " + desired.toLowerCase() + ".",
                        "Se a situação não parecer segura ou útil, use o percurso neutro ou decida não agir.");
        String headline = neutralPath
                ? "Sua escolha atual foi preservada"
                : "Seu primeiro ajuste possível está organizado";
        String summary = neutralPath
                ? "Você não precisa corrigir sua imagem. O MUSA registrou sua decisão e mantém a jornada disponível para organizar somente o que fizer sentido para você."
                : "Suas escolhas foram combinadas por regras locais. Teste um ajuste pequeno com o que já possui e avalie o resultado por você mesma.";
        String responseJson = serializeJson(Map.of(
                "headline", headline,
                "summary", summary,
                "signals", selectedSignals,
                "microActions", actions));
        return pending.withResult(
                "COMPLETED",
                headline,
                summary,
                selectedSignals,
                actions,
                "A orientação não avalia corpo, emoção, personalidade ou reação de terceiros.",
                LOCAL_RULES_MODEL,
                "LOCAL",
                serializeJson(Map.of("answers", pending.answers())),
                responseJson,
                0,
                0,
                BigDecimal.ZERO,
                "",
                Instant.now().toString());
    }

    /** Rejeita texto livre ou chave inesperada antes de persistir a versão categorial do MUSA. */
    private void validateMusaV7CategoricalAnswers(Map<String, String> answers) {
        MusaV7CategoricalContract.validate(answers);
    }

    /** Serializa auditoria local sem permitir que uma falha de log execute integração externa. */
    private String serializeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            log.error("Falha ao serializar auditoria das regras locais do MUSA v7", ex);
            throw new IllegalStateException("Não foi possível auditar a orientação local do MUSA v7", ex);
        }
    }

    /** Normaliza e limita respostas enviadas para personalização por IA. */
    private Map<String, String> sanitizeAnswers(Map<String, String> answers) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        if (answers == null) {
            return sanitized;
        }
        answers.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.trim();
            String normalizedValue = value == null ? "" : value.trim();
            if (!normalizedKey.isBlank() && !normalizedValue.isBlank()) {
                sanitized.put(normalizedKey, normalizedValue.length() > 2000
                        ? normalizedValue.substring(0, 2000)
                        : normalizedValue);
            }
        });
        return sanitized;
    }

    /** Coleta respostas anteriores para manter continuidade sem recomputar contexto no frontend. */
    private Map<String, String> previousMissionAnswers(WorkspaceResponse workspace, String currentMissionId) {
        Map<String, String> previousAnswers = new LinkedHashMap<>();
        workspace.missionInteractions().stream()
                .filter(answer -> !currentMissionId.equals(answer.missionId()))
                .forEach(answer -> previousAnswers.put(answer.questionKey(), answer.answerText()));
        return previousAnswers;
    }

    /** Converte uma pendência interna no contrato consumido pelo worker. */
    private AiGuidancePendingResponse toPendingResponse(StoredAiGuidance stored) {
        ProductExperienceResponse product = stored.accessToken().startsWith(PUBLIC_ACCESS_PREFIX)
                ? productCatalogService.getProduct(stored.productSlug())
                : accessService.getWorkspace(stored.accessToken()).product();
        return new AiGuidancePendingResponse(
                stored.requestId(),
                stored.productSlug(),
                stored.email(),
                stored.missionId(),
                stored.guidanceType(),
                STAGE_CODE,
                stored.status(),
                stored.answers(),
                stored.previousMissionAnswers(),
                product);
    }

    /** Converte o estado interno no contrato público do frontend. */
    private AiGuidanceResponse toResponse(StoredAiGuidance stored) {
        return new AiGuidanceResponse(
                stored.requestId(),
                stored.productSlug(),
                stored.missionId(),
                stored.guidanceType(),
                stored.status(),
                blankToNull(stored.headline()),
                blankToNull(stored.summary()),
                stored.signals(),
                stored.microActions(),
                blankToNull(stored.caution()),
                blankToNull(stored.model()),
                blankToNull(stored.serviceTier()),
                stored.inputTokens(),
                stored.outputTokens(),
                stored.costUsd(),
                blankToNull(stored.errorMessage()));
    }

    /** Busca a solicitação em memória ou no banco configurado. */
    private StoredAiGuidance getRequest(String requestId) {
        StoredAiGuidance stored = requestsById.get(requestId);
        if (stored != null) {
            return stored;
        }
        if (usesJdbcStorage()) {
            StoredAiGuidance loaded = loadGuidanceFromDatabase(requestId);
            if (loaded != null) {
                requestsById.put(requestId, loaded);
                return loaded;
            }
        }
        throw new IllegalArgumentException("Orientação PDE não encontrada");
    }

    /** Normaliza status aceitos para resultado do worker. */
    private String normalizeResultStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if ("COMPLETED".equals(normalized) || "FAILED".equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("Status de orientação PDE inválido: " + status);
    }

    /** Informa se o backend PDE deve persistir orientações no MySQL. */
    private boolean usesJdbcStorage() {
        return jdbcUrl != null && !jdbcUrl.isBlank();
    }

    /** Abre conexão JDBC para persistência da orientação. */
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    /** Carrega solicitações persistidas em arquivo ou banco ao iniciar o serviço. */
    private void loadPersistedRequests() {
        if (usesJdbcStorage()) {
            loadRecentGuidanceFromDatabase();
            return;
        }
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            Map<String, StoredAiGuidance> stored = objectMapper.readValue(storagePath.toFile(), STORE_TYPE);
            requestsById.putAll(stored);
        } catch (Exception ex) {
            log.error("Falha ao carregar orientações PDE por IA em {}", storagePath, ex);
            throw new IllegalStateException("Não foi possível carregar orientações PDE por IA", ex);
        }
    }

    /** Persiste uma solicitação de IA no armazenamento configurado. */
    private synchronized void persistRequest(StoredAiGuidance request) {
        if (usesJdbcStorage()) {
            databaseMigrationService.ensureAiGuidanceStorageReady();
            persistGuidanceInDatabase(request);
            return;
        }
        persistGuidanceInFile();
    }

    /** Persiste todas as solicitações de IA em arquivo local. */
    private synchronized void persistGuidanceInFile() {
        try {
            if (storagePath.getParent() != null) {
                Files.createDirectories(storagePath.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), requestsById);
        } catch (IOException ex) {
            log.error("Falha ao persistir orientações PDE por IA em {}", storagePath, ex);
            throw new IllegalStateException("Não foi possível persistir orientação PDE por IA", ex);
        }
    }

    /** Carrega orientações recentes do banco para acelerar consultas locais. */
    private void loadRecentGuidanceFromDatabase() {
        databaseMigrationService.ensureAiGuidanceStorageReady();
        String sql = """
                SELECT *
                FROM pde_ai_guidance_request
                ORDER BY created_at DESC
                LIMIT 500
                """;
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                StoredAiGuidance stored = fromResultSet(resultSet);
                requestsById.put(stored.requestId(), stored);
            }
        } catch (SQLException | IOException ex) {
            log.error("Falha ao carregar orientações PDE por IA no banco", ex);
            throw new IllegalStateException("Não foi possível carregar orientações PDE por IA no banco", ex);
        }
    }

    /** Carrega uma orientação específica do banco pelo identificador. */
    private StoredAiGuidance loadGuidanceFromDatabase(String requestId) {
        String sql = "SELECT * FROM pde_ai_guidance_request WHERE request_id = ?";
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requestId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? fromResultSet(resultSet) : null;
            }
        } catch (SQLException | IOException ex) {
            log.error("Falha ao buscar orientação PDE por IA no banco; requestId={}", requestId, ex);
            throw new IllegalStateException("Não foi possível buscar orientação PDE por IA", ex);
        }
    }

    /** Carrega a próxima orientação pendente do banco. */
    private Optional<StoredAiGuidance> loadPendingGuidanceFromDatabase() {
        String sql = """
                SELECT *
                FROM pde_ai_guidance_request
                WHERE status = 'PENDING'
                ORDER BY created_at ASC
                LIMIT 1
                """;
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next()) {
                return Optional.empty();
            }
            StoredAiGuidance stored = fromResultSet(resultSet);
            requestsById.put(stored.requestId(), stored);
            return Optional.of(stored);
        } catch (SQLException | IOException ex) {
            log.error("Falha ao buscar orientação PDE pendente para worker", ex);
            throw new IllegalStateException("Não foi possível buscar orientação PDE pendente", ex);
        }
    }

    /** Persiste ou atualiza uma orientação por IA no banco. */
    private void persistGuidanceInDatabase(StoredAiGuidance request) {
        String sql = """
                INSERT INTO pde_ai_guidance_request (
                  request_id, access_token, product_slug, email, mission_id, guidance_type, stage_code,
                  status, answers_json, previous_answers_json, headline, summary, signals_json,
                  micro_actions_json, caution, model, service_tier, raw_request_json, raw_response_json,
                  input_tokens, output_tokens, cost_usd, error_message, created_at, finished_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                  status = VALUES(status),
                  answers_json = VALUES(answers_json),
                  previous_answers_json = VALUES(previous_answers_json),
                  headline = VALUES(headline),
                  summary = VALUES(summary),
                  signals_json = VALUES(signals_json),
                  micro_actions_json = VALUES(micro_actions_json),
                  caution = VALUES(caution),
                  model = VALUES(model),
                  service_tier = VALUES(service_tier),
                  raw_request_json = VALUES(raw_request_json),
                  raw_response_json = VALUES(raw_response_json),
                  input_tokens = VALUES(input_tokens),
                  output_tokens = VALUES(output_tokens),
                  cost_usd = VALUES(cost_usd),
                  error_message = VALUES(error_message),
                  finished_at = VALUES(finished_at),
                  updated_at = CURRENT_TIMESTAMP
                """;
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.requestId());
            statement.setString(2, request.accessToken());
            statement.setString(3, request.productSlug());
            statement.setString(4, request.email());
            statement.setString(5, request.missionId());
            statement.setString(6, request.guidanceType());
            statement.setString(7, STAGE_CODE);
            statement.setString(8, request.status());
            statement.setString(9, objectMapper.writeValueAsString(request.answers()));
            statement.setString(10, objectMapper.writeValueAsString(request.previousMissionAnswers()));
            statement.setString(11, blankToNull(request.headline()));
            statement.setString(12, blankToNull(request.summary()));
            statement.setString(13, objectMapper.writeValueAsString(request.signals()));
            statement.setString(14, objectMapper.writeValueAsString(request.microActions()));
            statement.setString(15, blankToNull(request.caution()));
            statement.setString(16, blankToNull(request.model()));
            statement.setString(17, blankToNull(request.serviceTier()));
            statement.setString(18, blankToNull(request.rawRequestJson()));
            statement.setString(19, blankToNull(request.rawResponseJson()));
            setInteger(statement, 20, request.inputTokens());
            setInteger(statement, 21, request.outputTokens());
            statement.setBigDecimal(22, request.costUsd());
            statement.setString(23, blankToNull(request.errorMessage()));
            statement.setTimestamp(24, Timestamp.from(Instant.parse(request.createdAt())));
            if (blankToNull(request.finishedAt()) == null) {
                statement.setNull(25, java.sql.Types.TIMESTAMP);
            } else {
                statement.setTimestamp(25, Timestamp.from(Instant.parse(request.finishedAt())));
            }
            statement.executeUpdate();
        } catch (SQLException | IOException ex) {
            log.error("Falha ao persistir orientação PDE por IA no banco; requestId={}", request.requestId(), ex);
            throw new IllegalStateException("Não foi possível persistir orientação PDE por IA", ex);
        }
    }

    /** Reconstrói uma orientação a partir da linha lida do banco. */
    private StoredAiGuidance fromResultSet(ResultSet resultSet) throws SQLException, IOException {
        return new StoredAiGuidance(
                resultSet.getString("request_id"),
                resultSet.getString("access_token"),
                resultSet.getString("product_slug"),
                resultSet.getString("email"),
                resultSet.getString("mission_id"),
                resultSet.getString("guidance_type"),
                resultSet.getString("status"),
                readStringMap(resultSet.getString("answers_json")),
                readStringMap(resultSet.getString("previous_answers_json")),
                nullToBlank(resultSet.getString("headline")),
                nullToBlank(resultSet.getString("summary")),
                readStringList(resultSet.getString("signals_json")),
                readStringList(resultSet.getString("micro_actions_json")),
                nullToBlank(resultSet.getString("caution")),
                nullToBlank(resultSet.getString("model")),
                nullToBlank(resultSet.getString("service_tier")),
                nullToBlank(resultSet.getString("raw_request_json")),
                nullToBlank(resultSet.getString("raw_response_json")),
                (Integer) resultSet.getObject("input_tokens"),
                (Integer) resultSet.getObject("output_tokens"),
                resultSet.getBigDecimal("cost_usd"),
                nullToBlank(resultSet.getString("error_message")),
                resultSet.getTimestamp("created_at").toInstant().toString(),
                resultSet.getTimestamp("finished_at") == null
                        ? ""
                        : resultSet.getTimestamp("finished_at").toInstant().toString());
    }

    /** Lê um objeto JSON simples de texto para texto. */
    private Map<String, String> readStringMap(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    /** Lê uma lista JSON textual. */
    private List<String> readStringList(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    /** Preenche campo inteiro opcional em statement JDBC. */
    private void setInteger(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value);
    }

    /** Converte texto vazio em null para contratos públicos e persistência. */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** Converte texto nulo em vazio para armazenamento local. */
    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    /** Representa uma solicitação de orientação por IA persistida pelo backend PDE. */
    private record StoredAiGuidance(
            String requestId,
            String accessToken,
            String productSlug,
            String email,
            String missionId,
            String guidanceType,
            String status,
            Map<String, String> answers,
            Map<String, String> previousMissionAnswers,
            String headline,
            String summary,
            List<String> signals,
            List<String> microActions,
            String caution,
            String model,
            String serviceTier,
            String rawRequestJson,
            String rawResponseJson,
            Integer inputTokens,
            Integer outputTokens,
            BigDecimal costUsd,
            String errorMessage,
            String createdAt,
            String finishedAt) {

        /** Cria uma orientação pendente para execução pelo worker. */
        private static StoredAiGuidance pending(
                String requestId,
                String accessToken,
                String productSlug,
                String email,
                String missionId,
                String guidanceType,
                Map<String, String> answers,
                Map<String, String> previousMissionAnswers,
                String createdAt) {
            return new StoredAiGuidance(
                    requestId,
                    accessToken,
                    productSlug,
                    email,
                    missionId,
                    guidanceType,
                    "PENDING",
                    answers,
                    previousMissionAnswers,
                    "",
                    "",
                    List.of(),
                    List.of(),
                    "",
                    "",
                    "",
                    "",
                    "",
                    null,
                    null,
                    null,
                    "",
                    createdAt,
                    "");
        }

        /** Retorna uma nova versão da orientação com resultado do worker. */
        private StoredAiGuidance withResult(
                String status,
                String headline,
                String summary,
                List<String> signals,
                List<String> microActions,
                String caution,
                String model,
                String serviceTier,
                String rawRequestJson,
                String rawResponseJson,
                Integer inputTokens,
                Integer outputTokens,
                BigDecimal costUsd,
                String errorMessage,
                String finishedAt) {
            return new StoredAiGuidance(
                    requestId,
                    accessToken,
                    productSlug,
                    email,
                    missionId,
                    guidanceType,
                    status,
                    answers,
                    previousMissionAnswers,
                    headline,
                    summary,
                    signals,
                    microActions,
                    caution,
                    model,
                    serviceTier,
                    rawRequestJson,
                    rawResponseJson,
                    inputTokens,
                    outputTokens,
                    costUsd,
                    errorMessage,
                    createdAt,
                    finishedAt);
        }
    }
}
