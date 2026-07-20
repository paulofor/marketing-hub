package com.marketinghub.pde.service;

import com.marketinghub.pde.dto.AccessResponse;
import com.marketinghub.pde.dto.FunnelAnalyticsResetResponse;
import com.marketinghub.pde.dto.FunnelAnalyticsEventMetricDto;
import com.marketinghub.pde.dto.FunnelAnalyticsJourneyResponse;
import com.marketinghub.pde.dto.FunnelAnalyticsSessionJourneyDto;
import com.marketinghub.pde.dto.FunnelAnalyticsSessionStepDto;
import com.marketinghub.pde.dto.FunnelAnalyticsSummaryResponse;
import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.FunnelEventResponse;
import com.marketinghub.pde.dto.MagicLinkResponse;
import com.marketinghub.pde.dto.MissionInteractionRequest;
import com.marketinghub.pde.dto.MissionInteractionResponse;
import com.marketinghub.pde.dto.PepperWebhookRequest;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
    private final boolean requireJdbcStorage;
    private final String appBaseUrl;
    private final boolean exposeMagicLinkInResponse;
    private final PdeMailService mailService;
    private final GoogleIdentityService googleIdentityService;
    private final Map<String, AccessGrant> accessByToken = new ConcurrentHashMap<>();

    /** Recebe dependências e carrega os acessos persistidos em disco. */
    @Autowired
    public AccessService(
            ProductCatalogService productCatalogService,
            ObjectMapper objectMapper,
            @Value("${pde.access.storage-path:/data/pde/access-grants.json}") String storagePath,
            @Value("${pde.access.jdbc-url:}") String jdbcUrl,
            @Value("${pde.access.jdbc-username:}") String jdbcUsername,
            @Value("${pde.access.jdbc-password:}") String jdbcPassword,
            @Value("${pde.access.require-jdbc:false}") boolean requireJdbcStorage,
            @Value("${pde.access.app-base-url:http://localhost:5176}") String appBaseUrl,
            @Value("${pde.access.expose-magic-link-in-response:false}") boolean exposeMagicLinkInResponse,
            PdeMailService mailService,
            GoogleIdentityService googleIdentityService) {
        this.productCatalogService = productCatalogService;
        this.objectMapper = objectMapper;
        this.storagePath = Path.of(storagePath);
        this.jdbcUrl = jdbcUrl;
        this.jdbcUsername = jdbcUsername;
        this.jdbcPassword = jdbcPassword;
        this.requireJdbcStorage = requireJdbcStorage;
        this.appBaseUrl = appBaseUrl;
        this.exposeMagicLinkInResponse = exposeMagicLinkInResponse;
        this.mailService = mailService;
        this.googleIdentityService = googleIdentityService;
        validateJdbcStorageRequirement();
        loadPersistedAccess();
    }

    /** Recebe dependências para testes locais com persistência em arquivo. */
    public AccessService(ProductCatalogService productCatalogService, ObjectMapper objectMapper, String storagePath) {
        this(productCatalogService, objectMapper, storagePath, "", "", "", false, "http://localhost:5176", true, null, null);
    }

    /** Cria um acesso para um produto existente e retorna a URL da área da cliente. */
    public AccessResponse createAccess(String productSlug, String email, String source) {
        productCatalogService.getProduct(productSlug);
        AccessGrant existingGrant = findGrantByEmail(productSlug, email);
        if (existingGrant != null) {
            boolean promotedToPaid = shouldPromoteToPaidSource(existingGrant, source);
            if (promotedToPaid) {
                existingGrant.updateSource(source);
                persistAccess(existingGrant);
                recordSubscriptionApprovedIfNeeded(existingGrant, source, Map.of());
            }
            return toAccessResponse(existingGrant);
        }
        String token = UUID.randomUUID().toString();
        AccessGrant grant = new AccessGrant(token, productSlug, normalizeEmail(email), source, Instant.now());
        accessByToken.put(token, grant);
        persistAccess(grant);
        recordSubscriptionApprovedIfNeeded(grant, source, Map.of());
        return toAccessResponse(grant);
    }

    /** Processa o webhook Pepper e libera acesso apenas quando o pagamento foi realizado. */
    public AccessResponse receivePepperWebhook(PepperWebhookRequest request) {
        String status = request.resolvedStatus();
        String transactionId = request.resolvedTransactionId();
        String buyerEmail = request.resolvedBuyerEmail();
        String productSlug = request.resolvedProductSlug("metodo-musa-7-dias");
        if (!"paid".equalsIgnoreCase(status)) {
            log.info(
                    "Webhook Pepper ignorado sem pagamento realizado; productSlug={}, transactionId={}, status={}",
                    productSlug,
                    transactionId,
                    status);
            throw new IllegalArgumentException("Webhook Pepper sem pagamento realizado: " + status);
        }
        if (buyerEmail == null || buyerEmail.isBlank()) {
            throw new IllegalArgumentException("Webhook Pepper sem e-mail da compradora");
        }
        log.info(
                "Webhook Pepper aprovado para liberar acesso PDE; productSlug={}, transactionId={}, buyerEmail={}",
                productSlug,
                transactionId,
                buyerEmail);
        return releasePepperPaidTransaction(
                productSlug,
                buyerEmail,
                transactionId,
                request.offer() == null ? null : request.offer().hash());
    }

    /** Libera compra paga consultada na Pepper quando o postback nao foi entregue. */
    public AccessResponse releasePepperPaidTransaction(
            String productSlug, String buyerEmail, String transactionId, String offerHash) {
        productCatalogService.getProduct(productSlug);
        if (buyerEmail == null || buyerEmail.isBlank()) {
            throw new IllegalArgumentException("Transacao Pepper sem e-mail da compradora");
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (transactionId != null && !transactionId.isBlank()) {
            metadata.put("pepperTransactionId", transactionId);
        }
        if (offerHash != null && !offerHash.isBlank()) {
            metadata.put("pepperOfferHash", offerHash);
        }
        AccessGrant existingGrant = findGrantByEmail(productSlug, buyerEmail);
        if (existingGrant != null) {
            boolean promotedToPaid = shouldPromoteToPaidSource(existingGrant, "PEPPER");
            if (promotedToPaid) {
                existingGrant.updateSource("PEPPER");
                persistAccess(existingGrant);
                recordSubscriptionApprovedIfNeeded(existingGrant, "PEPPER", metadata);
            }
            return toAccessResponse(existingGrant);
        }
        String token = UUID.randomUUID().toString();
        AccessGrant grant = new AccessGrant(token, productSlug, normalizeEmail(buyerEmail), "PEPPER", Instant.now());
        accessByToken.put(token, grant);
        persistAccess(grant);
        recordSubscriptionApprovedIfNeeded(grant, "PEPPER", metadata);
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
            throw new IllegalArgumentException("Cadastro da Área MUSA não encontrado para este e-mail");
        }
        recordFunnelEvent(new FunnelEventRequest(
                productSlug,
                "LOGIN_COMPLETED",
                grant.getToken(),
                grant.getEmail(),
                "EMAIL",
                "pde-platform",
                null,
                Map.of("method", "legacy_email_login")));
        return toAccessResponse(grant);
    }

    /** Gera ou reutiliza o acesso e envia um link mágico para o e-mail da cliente. */
    public MagicLinkResponse requestMagicLink(String productSlug, String email) {
        AccessResponse access = createAccess(productSlug, email, "MAGIC_LINK");
        return sendAccessLink(productSlug, access.email(), access.accessUrl());
    }

    /** Envia link mágico apenas quando já existe cadastro para o e-mail informado. */
    public MagicLinkResponse requestExistingMagicLink(String productSlug, String email) {
        productCatalogService.getProduct(productSlug);
        AccessGrant grant = findGrantByEmail(productSlug, email);
        if (grant == null) {
            throw new IllegalArgumentException("Cadastro da Área MUSA não encontrado para este e-mail");
        }
        recordFunnelEvent(new FunnelEventRequest(
                productSlug,
                "LOGIN_COMPLETED",
                grant.getToken(),
                grant.getEmail(),
                "EMAIL_MAGIC_LINK",
                "pde-platform",
                null,
                Map.of("method", "existing_customer_magic_link")));
        return sendAccessLink(productSlug, grant.getEmail(), "/access/" + grant.getToken());
    }

    /** Autentica ou cria acesso da cliente validada pelo Google. */
    public AccessResponse loginWithGoogle(String productSlug, String idToken) {
        if (googleIdentityService == null) {
            throw new IllegalArgumentException("Login com Google ainda não configurado para a Área MUSA");
        }
        String verifiedEmail = googleIdentityService.verifyEmail(idToken);
        AccessResponse access = createAccess(productSlug, verifiedEmail, "GOOGLE");
        recordFunnelEvent(new FunnelEventRequest(
                productSlug,
                "LOGIN_COMPLETED",
                access.token(),
                access.email(),
                "GOOGLE",
                "pde-platform",
                null,
                Map.of("method", "google_identity")));
        return access;
    }

    /** Registra um evento comercial da jornada PED/MUSA para medição do funil. */
    public FunnelEventResponse recordFunnelEvent(FunnelEventRequest request) {
        productCatalogService.getProduct(request.productSlug());
        String normalizedEventType = normalizeEventType(request.eventType());
        String eventId = UUID.randomUUID().toString();
        if (usesJdbcStorage()) {
            persistFunnelEventInDatabase(eventId, request, normalizedEventType);
        } else {
            log.info(
                    "Evento PDE registrado sem persistência JDBC; eventId={}, productSlug={}, eventType={}, accessToken={}",
                    eventId,
                    request.productSlug(),
                    normalizedEventType,
                    request.accessToken());
        }
        return new FunnelEventResponse(eventId, normalizedEventType, "RECORDED");
    }

    /** Consolida métricas de funil e analytics para decisão de próximas campanhas. */
    public FunnelAnalyticsSummaryResponse summarizeFunnelAnalytics(String productSlug) {
        productCatalogService.getProduct(productSlug);
        if (!usesJdbcStorage()) {
            return emptyFunnelAnalytics(productSlug);
        }
        String summarySql = """
                SELECT
                  COUNT(*) AS total_events,
                  COUNT(DISTINCT visitor_id) AS unique_visitors,
                  COUNT(DISTINCT session_id) AS sessions,
                  SUM(CASE WHEN event_type = 'PED_ENTRY' THEN 1 ELSE 0 END) AS ped_entries,
                  SUM(CASE WHEN event_type = 'PAGE_VIEW' THEN 1 ELSE 0 END) AS page_views,
                  SUM(CASE WHEN event_type = 'LOGIN_STARTED' THEN 1 ELSE 0 END) AS login_started,
                  SUM(CASE WHEN event_type = 'LOGIN_COMPLETED' THEN 1 ELSE 0 END) AS login_completed,
                  SUM(CASE WHEN event_type = 'PAYWALL_VIEWED' THEN 1 ELSE 0 END) AS paywall_viewed,
                  SUM(CASE WHEN event_type = 'SUBSCRIPTION_CLICKED' THEN 1 ELSE 0 END) AS subscription_clicked,
                  SUM(CASE WHEN event_type = 'SUBSCRIPTION_APPROVED' THEN 1 ELSE 0 END) AS subscription_approved,
                  SUM(CASE WHEN event_type = 'ACCESS_RELEASED' THEN 1 ELSE 0 END) AS access_released,
                  SUM(CASE WHEN event_type = 'FIRST_USE' THEN 1 ELSE 0 END) AS first_use,
                  SUM(CASE WHEN event_type = 'CHECKOUT_STARTED' THEN 1 ELSE 0 END) AS checkout_started,
                  COALESCE(SUM(visible_ms), 0) AS total_visible_ms
                FROM pde_funnel_event
                WHERE product_slug = ?
                """;
        try (Connection connection = openConnection();
                PreparedStatement summaryStatement = connection.prepareStatement(summarySql)) {
            summaryStatement.setString(1, productSlug);
            try (ResultSet resultSet = summaryStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new FunnelAnalyticsSummaryResponse(
                            productSlug,
                            resultSet.getLong("total_events"),
                            resultSet.getLong("unique_visitors"),
                            resultSet.getLong("sessions"),
                            resultSet.getLong("ped_entries"),
                            resultSet.getLong("page_views"),
                            resultSet.getLong("login_started"),
                            resultSet.getLong("login_completed"),
                            resultSet.getLong("paywall_viewed"),
                            resultSet.getLong("subscription_clicked"),
                            resultSet.getLong("subscription_approved"),
                            resultSet.getLong("access_released"),
                            resultSet.getLong("first_use"),
                            resultSet.getLong("checkout_started"),
                            resultSet.getLong("total_visible_ms"),
                            loadFunnelEventMetrics(connection, productSlug));
                }
            }
        } catch (SQLException ex) {
            log.error("Falha ao consolidar analytics PDE; productSlug={}", productSlug, ex);
            throw new IllegalStateException("Não foi possível consolidar analytics PDE", ex);
        }
        return emptyFunnelAnalytics(productSlug);
    }

    /** Consolida jornadas recentes por sessão para diagnosticar onde a visitante abandonou. */
    public FunnelAnalyticsJourneyResponse summarizeSessionJourneys(String productSlug, int limit) {
        productCatalogService.getProduct(productSlug);
        if (!usesJdbcStorage()) {
            return new FunnelAnalyticsJourneyResponse(productSlug, 0, List.of());
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        String sql = """
                SELECT
                  COALESCE(e.session_id, e.event_id) AS resolved_session_id,
                  e.visitor_id,
                  e.event_type,
                  e.page_url,
                  e.visible_ms,
                  e.section_id,
                  e.action_name,
                  e.metadata_json,
                  e.occurred_at
                FROM pde_funnel_event e
                JOIN (
                  SELECT
                    COALESCE(session_id, event_id) AS resolved_session_id,
                    MAX(occurred_at) AS last_event_at
                  FROM pde_funnel_event
                  WHERE product_slug = ?
                  GROUP BY COALESCE(session_id, event_id)
                  ORDER BY last_event_at DESC
                  LIMIT ?
                ) recent ON recent.resolved_session_id = COALESCE(e.session_id, e.event_id)
                WHERE e.product_slug = ?
                ORDER BY recent.last_event_at DESC, e.occurred_at ASC, e.id ASC
                """;
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productSlug);
            statement.setInt(2, safeLimit);
            statement.setString(3, productSlug);
            try (ResultSet resultSet = statement.executeQuery()) {
                Map<String, SessionJourneyBuilder> builders = new LinkedHashMap<>();
                while (resultSet.next()) {
                    String sessionId = resultSet.getString("resolved_session_id");
                    SessionJourneyBuilder builder = builders.computeIfAbsent(sessionId, SessionJourneyBuilder::new);
                    builder.add(toSessionJourneyEvent(resultSet));
                }
                List<FunnelAnalyticsSessionJourneyDto> sessions = builders.values().stream()
                        .map(SessionJourneyBuilder::toDto)
                        .toList();
                return new FunnelAnalyticsJourneyResponse(productSlug, sessions.size(), sessions);
            }
        } catch (SQLException | IOException ex) {
            log.error("Falha ao consolidar jornadas PDE; productSlug={}, limit={}", productSlug, safeLimit, ex);
            throw new IllegalStateException("Não foi possível consolidar jornadas PDE", ex);
        }
    }

    /** Apaga eventos analíticos/testes do produto antes de iniciar leitura de campanha paga real. */
    public FunnelAnalyticsResetResponse resetFunnelAnalyticsForCampaignStart(String productSlug) {
        productCatalogService.getProduct(productSlug);
        if (!usesJdbcStorage()) {
            log.info("Reset de analytics PDE ignorado sem JDBC; productSlug={}", productSlug);
            return new FunnelAnalyticsResetResponse(productSlug, 0, "SKIPPED_NO_JDBC");
        }
        String sql = "DELETE FROM pde_funnel_event WHERE product_slug = ?";
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productSlug);
            int deletedEvents = statement.executeUpdate();
            log.info(
                    "Reset de analytics PDE para inicio de campanha; productSlug={}, deletedEvents={}",
                    productSlug,
                    deletedEvents);
            return new FunnelAnalyticsResetResponse(productSlug, deletedEvents, "RESET");
        } catch (SQLException ex) {
            log.error("Falha ao limpar analytics PDE para inicio de campanha; productSlug={}", productSlug, ex);
            throw new IllegalStateException("Não foi possível limpar analytics PDE", ex);
        }
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
                grant.getSource(),
                resolveSubscriptionStatus(grant),
                completedMissions,
                totalMissions,
                progressPercent,
                completedMissionIds.stream().toList(),
                toMissionInteractionResponses(grant));
    }

    /** Marca uma missão do produto como concluída após validar se ela existe. */
    public void completeMission(String token, String missionId) {
        AccessGrant grant = getGrant(token);
        validateMissionExists(grant, missionId);
        boolean firstCompletedMission = grant.getCompletedMissionIds().isEmpty();
        grant.completeMission(missionId);
        persistAccess(grant);
        if (firstCompletedMission) {
            recordFunnelEvent(new FunnelEventRequest(
                    grant.getProductSlug(),
                    "FIRST_USE",
                    grant.getToken(),
                    grant.getEmail(),
                    grant.getSource(),
                    "pde-platform",
                    null,
                    Map.of("activationType", "mission_completion", "missionId", missionId)));
        }
    }

    /** Salva respostas da cliente para personalizar a missão e medir engajamento real. */
    public void saveMissionInteraction(String token, String missionId, MissionInteractionRequest request) {
        AccessGrant grant = getGrant(token);
        validateMissionExists(grant, missionId);
        Map<String, String> sanitizedAnswers = sanitizeInteractionAnswers(request.answers());
        grant.saveMissionInteraction(missionId, sanitizedAnswers);
        persistAccess(grant);
        recordFunnelEvent(new FunnelEventRequest(
                grant.getProductSlug(),
                "MISSION_INTERACTION_SAVED",
                grant.getToken(),
                grant.getEmail(),
                grant.getSource(),
                "pde-platform",
                null,
                Map.of("missionId", missionId, "answerKeys", sanitizedAnswers.keySet())));
    }

    /** Confirma que a missão pertence ao produto acessado pela cliente. */
    private void validateMissionExists(AccessGrant grant, String missionId) {
        ProductExperienceResponse product = productCatalogService.getProduct(grant.getProductSlug());
        boolean missionExists = product.missions().stream().anyMatch(mission -> mission.id().equals(missionId));
        if (!missionExists) {
            throw new IllegalArgumentException("Missão PDE não encontrada: " + missionId);
        }
    }

    /** Normaliza respostas livres antes de salvar no perfil de personalização da missão. */
    private Map<String, String> sanitizeInteractionAnswers(Map<String, String> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma resposta da missão");
        }
        Map<String, String> sanitized = new LinkedHashMap<>();
        answers.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.trim();
            String normalizedValue = value == null ? "" : value.trim();
            if (normalizedKey.isBlank() || normalizedKey.length() > 100) {
                throw new IllegalArgumentException("Chave de interação PDE inválida");
            }
            if (normalizedValue.isBlank()) {
                return;
            }
            if (normalizedValue.length() > 2000) {
                throw new IllegalArgumentException("Resposta da interação PDE acima do limite");
            }
            sanitized.put(normalizedKey, normalizedValue);
        });
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma resposta preenchida da missão");
        }
        return sanitized;
    }

    /** Converte as respostas salvas para o contrato público da workspace. */
    private List<MissionInteractionResponse> toMissionInteractionResponses(AccessGrant grant) {
        List<MissionInteractionResponse> responses = new java.util.ArrayList<>();
        grant.getMissionInteractions().forEach((missionId, answers) ->
                answers.forEach((questionKey, answerText) ->
                        responses.add(new MissionInteractionResponse(missionId, questionKey, answerText))));
        return responses;
    }

    /** Busca o acesso pelo token ou falha quando ele não existir. */
    private AccessGrant getGrant(String token) {
        AccessGrant grant = accessByToken.get(token);
        if (grant == null) {
            throw new IllegalArgumentException("Acesso PDE não encontrado");
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

    /** Resolve se o acesso atual representa assinatura ativa ou apenas entrada/logon na área. */
    private String resolveSubscriptionStatus(AccessGrant grant) {
        return "CHECKOUT".equalsIgnoreCase(grant.getSource()) || "PEPPER".equalsIgnoreCase(grant.getSource())
                ? "ACTIVE"
                : "TRIAL";
    }

    /** Converte URL relativa em URL absoluta para envio por e-mail. */
    private String buildAbsoluteAccessUrl(String accessUrl) {
        String normalizedBase = appBaseUrl == null || appBaseUrl.isBlank()
                ? "http://localhost:5176"
                : appBaseUrl.replaceAll("/+$", "");
        return normalizedBase + accessUrl;
    }

    /** Envia ou expõe em teste o link de acesso da Área MUSA. */
    private MagicLinkResponse sendAccessLink(String productSlug, String email, String accessUrl) {
        String absoluteUrl = buildAbsoluteAccessUrl(accessUrl);
        if (mailService != null && mailService.isConfigured()) {
            try {
                mailService.sendMagicLink(email, absoluteUrl);
                return new MagicLinkResponse(productSlug, email, "SENT", null);
            } catch (RuntimeException ex) {
                log.error(
                        "Falha ao entregar link mágico PDE; productSlug={}, email={}, accessUrl={}",
                        productSlug,
                        email,
                        accessUrl,
                        ex);
                return new MagicLinkResponse(
                        productSlug,
                        email,
                        "EMAIL_SEND_FAILED",
                        exposeMagicLinkInResponse ? accessUrl : null);
            }
        }
        return new MagicLinkResponse(
                productSlug,
                email,
                "EMAIL_NOT_CONFIGURED",
                exposeMagicLinkInResponse ? accessUrl : null);
    }

    /** Registra compra ou assinatura aprovada quando a origem representa checkout real. */
    private void recordSubscriptionApprovedIfNeeded(AccessGrant grant, String source, Map<String, Object> metadata) {
        if ("CHECKOUT".equalsIgnoreCase(source) || "PEPPER".equalsIgnoreCase(source)) {
            Map<String, Object> eventMetadata = new LinkedHashMap<>();
            eventMetadata.put("accessSource", source);
            if (metadata != null) {
                eventMetadata.putAll(metadata);
            }
            recordFunnelEvent(new FunnelEventRequest(
                    grant.getProductSlug(),
                    "SUBSCRIPTION_APPROVED",
                    grant.getToken(),
                    grant.getEmail(),
                    source,
                    "pde-platform",
                    null,
                    eventMetadata));
            recordFunnelEvent(new FunnelEventRequest(
                    grant.getProductSlug(),
                    "ACCESS_RELEASED",
                    grant.getToken(),
                    grant.getEmail(),
                    source,
                    "pde-platform",
                    null,
                    eventMetadata));
        }
    }

    /** Define se um acesso gratuito deve ser promovido para origem de assinatura paga. */
    private boolean shouldPromoteToPaidSource(AccessGrant grant, String source) {
        return ("CHECKOUT".equalsIgnoreCase(source) || "PEPPER".equalsIgnoreCase(source))
                && !"CHECKOUT".equalsIgnoreCase(grant.getSource())
                && !"PEPPER".equalsIgnoreCase(grant.getSource());
    }

    /** Normaliza e valida os tipos de evento aceitos pelo funil MUSA/PDE. */
    private String normalizeEventType(String eventType) {
        String normalized = eventType == null ? "" : eventType.trim().toUpperCase();
        Set<String> allowed = Set.of(
                "PED_ENTRY",
                "PAGE_VIEW",
                "PAGE_LOAD",
                "PAGE_VISIBLE_TIME",
                "SCREEN_VIEW",
                "SCREEN_TIME",
                "SECTION_VIEW",
                "SCROLL_DEPTH",
                "CTA_VIEWED",
                "UI_CLICK",
                "LINK_CLICK",
                "FIELD_FOCUS",
                "FIELD_INPUT",
                "FIELD_FILLED",
                "FIELD_ABANDONED",
                "LOGIN_STARTED",
                "LOGIN_COMPLETED",
                "PAYWALL_VIEWED",
                "SUBSCRIPTION_CLICKED",
                "CHECKOUT_STARTED",
                "SUBSCRIPTION_APPROVED",
                "ACCESS_RELEASED",
                "FIRST_USE",
                "MISSION_OPEN",
                "MISSION_COMPLETED",
                "MISSION_INTERACTION_SAVED",
                "AI_GUIDANCE_REQUESTED",
                "MATERIAL_OPEN");
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Evento PDE não suportado: " + eventType);
        }
        return normalized;
    }

    /** Retorna métricas vazias quando o backend está em modo local sem banco analítico. */
    private FunnelAnalyticsSummaryResponse emptyFunnelAnalytics(String productSlug) {
        return new FunnelAnalyticsSummaryResponse(productSlug, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of());
    }

    /** Converte uma linha JDBC em evento normalizado de jornada. */
    private SessionJourneyEvent toSessionJourneyEvent(ResultSet resultSet) throws SQLException, IOException {
        Map<String, Object> metadata = readMetadata(resultSet.getString("metadata_json"));
        return new SessionJourneyEvent(
                resultSet.getString("visitor_id"),
                resultSet.getString("event_type"),
                resultSet.getString("page_url"),
                nullableLong(resultSet, "visible_ms"),
                resultSet.getString("section_id"),
                resultSet.getString("action_name"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                metadataString(metadata, "screenName"),
                metadataLong(metadata, "scrollDepthPercent"),
                metadataLong(metadata, "maxScrollDepthPercent"),
                metadataString(metadata, "fieldName"),
                metadataString(metadata, "elementText"));
    }

    /** Lê o JSON de metadados salvo no evento para detalhar tela, campo e clique. */
    private Map<String, Object> readMetadata(String metadataJson) throws IOException {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        return objectMapper.readValue(metadataJson, new TypeReference<>() {});
    }

    /** Lê valor longo nullable preservando diferença entre zero e null. */
    private Long nullableLong(ResultSet resultSet, String columnName) throws SQLException {
        long value = resultSet.getLong(columnName);
        return resultSet.wasNull() ? null : value;
    }

    /** Lê as contagens por tipo de evento para detalhar o relatório comercial. */
    private List<FunnelAnalyticsEventMetricDto> loadFunnelEventMetrics(Connection connection, String productSlug)
            throws SQLException {
        String sql = """
                SELECT event_type, COUNT(*) AS total
                FROM pde_funnel_event
                WHERE product_slug = ?
                GROUP BY event_type
                ORDER BY total DESC, event_type
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productSlug);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<FunnelAnalyticsEventMetricDto> metrics = new java.util.ArrayList<>();
                while (resultSet.next()) {
                    metrics.add(new FunnelAnalyticsEventMetricDto(
                            resultSet.getString("event_type"),
                            resultSet.getLong("total")));
                }
                return metrics;
            }
        }
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
            throw new IllegalStateException("Não foi possível carregar acessos PDE persistidos", ex);
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
            throw new IllegalStateException("Não foi possível persistir acesso PDE", ex);
        }
    }

    /** Informa se o backend PDE deve usar o banco MySQL do Marketing Hub. */
    private boolean usesJdbcStorage() {
        return jdbcUrl != null && !jdbcUrl.isBlank();
    }

    /** Bloqueia execução comercial quando a produção exigir persistência JDBC configurada. */
    private void validateJdbcStorageRequirement() {
        if ((requireJdbcStorage || isCommercialProductionUrl())
                && (!usesJdbcStorage()
                        || jdbcUsername == null
                        || jdbcUsername.isBlank()
                        || jdbcPassword == null
                        || jdbcPassword.isBlank())) {
            throw new IllegalStateException(
                    "Persistência JDBC do PDE é obrigatória neste ambiente; configure URL, usuário e senha JDBC");
        }
    }

    /** Identifica URLs comerciais públicas que não podem operar em modo local sem banco. */
    private boolean isCommercialProductionUrl() {
        return appBaseUrl != null && appBaseUrl.toLowerCase().contains("clubemusa.com.br");
    }

    /** Abre conexão direta com o banco configurado para o PDE. */
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }

    /** Carrega acessos, missões concluídas e interações a partir do MySQL do Marketing Hub. */
    private void loadPersistedAccessFromDatabase() {
        String sql = """
                SELECT
                  g.token,
                  g.product_slug,
                  g.email,
                  g.source,
                  g.created_at,
                  c.mission_id AS completed_mission_id,
                  a.mission_id AS interaction_mission_id,
                  a.question_key,
                  a.answer_text
                FROM pde_access_grant g
                LEFT JOIN pde_access_mission_completion c ON c.access_token = g.token
                LEFT JOIN pde_access_mission_interaction_answer a ON a.access_token = g.token
                ORDER BY g.created_at, c.completed_at, a.updated_at
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
                String missionId = resultSet.getString("completed_mission_id");
                if (missionId != null && !missionId.isBlank()) {
                    builder.completedMissionIds().add(missionId);
                }
                String interactionMissionId = resultSet.getString("interaction_mission_id");
                String questionKey = resultSet.getString("question_key");
                String answerText = resultSet.getString("answer_text");
                if (interactionMissionId != null && questionKey != null && answerText != null) {
                    builder.missionInteractions()
                            .computeIfAbsent(interactionMissionId, ignored -> new LinkedHashMap<>())
                            .put(questionKey, answerText);
                }
            }
            builders.forEach((token, builder) -> accessByToken.put(token, builder.toAccessGrant(token)));
        } catch (SQLException ex) {
            log.error("Falha ao carregar acessos PDE no banco Marketing Hub", ex);
            throw new IllegalStateException("Não foi possível carregar acessos PDE no banco Marketing Hub", ex);
        }
    }

    /** Persiste o acesso alterado, suas missões concluídas e interações no MySQL do Marketing Hub. */
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
        String upsertInteraction = """
                INSERT INTO pde_access_mission_interaction_answer (
                  access_token, product_slug, mission_id, question_key, answer_text, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                  answer_text = VALUES(answer_text),
                  updated_at = CURRENT_TIMESTAMP
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
            try (PreparedStatement statement = connection.prepareStatement(upsertInteraction)) {
                for (Map.Entry<String, Map<String, String>> missionEntry : grant.getMissionInteractions().entrySet()) {
                    for (Map.Entry<String, String> answerEntry : missionEntry.getValue().entrySet()) {
                        statement.setString(1, grant.getToken());
                        statement.setString(2, grant.getProductSlug());
                        statement.setString(3, missionEntry.getKey());
                        statement.setString(4, answerEntry.getKey());
                        statement.setString(5, answerEntry.getValue());
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
            connection.commit();
        } catch (SQLException ex) {
            log.error("Falha ao persistir acesso PDE no banco Marketing Hub; token={}", grant.getToken(), ex);
            throw new IllegalStateException("Não foi possível persistir acesso PDE no banco Marketing Hub", ex);
        }
    }

    /** Persiste evento comercial PED/MUSA no banco Marketing Hub. */
    private void persistFunnelEventInDatabase(String eventId, FunnelEventRequest request, String eventType) {
        String sql = """
                INSERT INTO pde_funnel_event (
                  event_id, product_slug, access_token, email, normalized_email, event_type,
                  provider, source, page_url, referrer_url, session_id, visitor_id,
                  utm_source, utm_medium, utm_campaign, utm_content, utm_term,
                  device_type, screen_width, screen_height, viewport_width, viewport_height,
                  visible_ms, section_id, action_name, metadata_json, occurred_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        try (Connection connection = openConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            Map<String, Object> metadata = request.metadata();
            statement.setString(1, eventId);
            statement.setString(2, request.productSlug());
            statement.setString(3, blankToNull(request.accessToken()));
            statement.setString(4, blankToNull(request.email()));
            statement.setString(5, blankToNull(normalizeEmail(request.email())));
            statement.setString(6, eventType);
            statement.setString(7, blankToNull(request.provider()));
            statement.setString(8, blankToNull(request.source()));
            statement.setString(9, blankToNull(request.pageUrl()));
            statement.setString(10, blankToNull(metadataString(metadata, "referrerUrl")));
            statement.setString(11, blankToNull(metadataString(metadata, "sessionId")));
            statement.setString(12, blankToNull(metadataString(metadata, "visitorId")));
            statement.setString(13, blankToNull(metadataString(metadata, "utmSource")));
            statement.setString(14, blankToNull(metadataString(metadata, "utmMedium")));
            statement.setString(15, blankToNull(metadataString(metadata, "utmCampaign")));
            statement.setString(16, blankToNull(metadataString(metadata, "utmContent")));
            statement.setString(17, blankToNull(metadataString(metadata, "utmTerm")));
            statement.setString(18, blankToNull(metadataString(metadata, "deviceType")));
            setInteger(statement, 19, metadataLong(metadata, "screenWidth"));
            setInteger(statement, 20, metadataLong(metadata, "screenHeight"));
            setInteger(statement, 21, metadataLong(metadata, "viewportWidth"));
            setInteger(statement, 22, metadataLong(metadata, "viewportHeight"));
            setLong(statement, 23, metadataLong(metadata, "visibleMs"));
            statement.setString(24, blankToNull(metadataString(metadata, "sectionId")));
            statement.setString(25, blankToNull(metadataString(metadata, "actionName")));
            statement.setString(26, metadata == null ? null : objectMapper.writeValueAsString(metadata));
            statement.executeUpdate();
        } catch (SQLException | IOException ex) {
            log.error(
                    "Falha ao persistir evento PDE no banco Marketing Hub; eventId={}, productSlug={}, eventType={}",
                    eventId,
                    request.productSlug(),
                    eventType,
                    ex);
            throw new IllegalStateException("Não foi possível persistir evento PDE no banco Marketing Hub", ex);
        }
    }

    /** Converte texto vazio em null para persistência normalizada. */
    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    /** Lê um campo textual opcional do metadado do evento. */
    private String metadataString(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /** Lê um campo numérico opcional do metadado do evento. */
    private Long metadataLong(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                log.warn("Metadado numerico PDE invalido ignorado; key={}, value={}", key, text);
            }
        }
        return null;
    }

    /** Preenche campo inteiro opcional em statement JDBC. */
    private void setInteger(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.INTEGER);
            return;
        }
        statement.setInt(index, value.intValue());
    }

    /** Preenche campo longo opcional em statement JDBC. */
    private void setLong(PreparedStatement statement, int index, Long value) throws SQLException {
        if (value == null) {
            statement.setNull(index, java.sql.Types.BIGINT);
            return;
        }
        statement.setLong(index, value);
    }

    /** Evento interno já enriquecido para compor uma jornada individual. */
    private record SessionJourneyEvent(
            String visitorId,
            String eventType,
            String pageUrl,
            Long visibleMs,
            String sectionId,
            String actionName,
            Instant occurredAt,
            String screenName,
            Long scrollDepthPercent,
            Long maxScrollDepthPercent,
            String fieldName,
            String elementText) {}

    /** Acumula eventos de uma sessão e calcula sinais comerciais de abandono. */
    private static class SessionJourneyBuilder {
        private final String sessionId;
        private final List<FunnelAnalyticsSessionStepDto> steps = new ArrayList<>();
        private final Set<String> screenNames = new LinkedHashSet<>();
        private final Set<String> sectionIds = new LinkedHashSet<>();
        private String visitorId;
        private Instant firstEventAt;
        private Instant lastEventAt;
        private long totalVisibleMs;
        private long maxScrollDepthPercent;
        private boolean fieldFocused;
        private boolean fieldInputStarted;
        private boolean fieldFilled;
        private boolean ctaClicked;
        private boolean loginStarted;
        private boolean loginCompleted;
        private boolean paywallViewed;
        private boolean checkoutStarted;
        private boolean subscriptionApproved;
        private String lastEventType;
        private String lastActionName;

        /** Cria acumulador para uma sessão. */
        private SessionJourneyBuilder(String sessionId) {
            this.sessionId = sessionId;
        }

        /** Adiciona evento e atualiza os indicadores da sessão. */
        private void add(SessionJourneyEvent event) {
            if (visitorId == null || visitorId.isBlank()) {
                visitorId = event.visitorId();
            }
            firstEventAt = firstEventAt == null || event.occurredAt().isBefore(firstEventAt)
                    ? event.occurredAt()
                    : firstEventAt;
            lastEventAt = lastEventAt == null || event.occurredAt().isAfter(lastEventAt)
                    ? event.occurredAt()
                    : lastEventAt;
            totalVisibleMs += event.visibleMs() == null ? 0 : event.visibleMs();
            Long eventScrollDepth = event.maxScrollDepthPercent() != null
                    ? event.maxScrollDepthPercent()
                    : event.scrollDepthPercent();
            maxScrollDepthPercent = Math.max(maxScrollDepthPercent, eventScrollDepth == null ? 0 : eventScrollDepth);
            addIfPresent(screenNames, event.screenName());
            addIfPresent(sectionIds, event.sectionId());
            fieldFocused = fieldFocused || "FIELD_FOCUS".equals(event.eventType());
            fieldInputStarted = fieldInputStarted || "FIELD_INPUT".equals(event.eventType());
            fieldFilled = fieldFilled || "FIELD_FILLED".equals(event.eventType());
            ctaClicked = ctaClicked || isCtaClick(event);
            loginStarted = loginStarted || "LOGIN_STARTED".equals(event.eventType());
            loginCompleted = loginCompleted || "LOGIN_COMPLETED".equals(event.eventType());
            paywallViewed = paywallViewed || "PAYWALL_VIEWED".equals(event.eventType());
            checkoutStarted = checkoutStarted || "CHECKOUT_STARTED".equals(event.eventType());
            subscriptionApproved = subscriptionApproved || "SUBSCRIPTION_APPROVED".equals(event.eventType());
            lastEventType = event.eventType();
            lastActionName = event.actionName();
            steps.add(new FunnelAnalyticsSessionStepDto(
                    event.occurredAt().toString(),
                    event.eventType(),
                    event.screenName(),
                    event.sectionId(),
                    event.actionName(),
                    event.visibleMs(),
                    eventScrollDepth,
                    event.fieldName(),
                    event.elementText(),
                    event.pageUrl()));
        }

        /** Monta o DTO público da jornada. */
        private FunnelAnalyticsSessionJourneyDto toDto() {
            return new FunnelAnalyticsSessionJourneyDto(
                    sessionId,
                    visitorId,
                    firstEventAt == null ? null : firstEventAt.toString(),
                    lastEventAt == null ? null : lastEventAt.toString(),
                    totalVisibleMs,
                    maxScrollDepthPercent,
                    List.copyOf(screenNames),
                    List.copyOf(sectionIds),
                    fieldFocused,
                    fieldInputStarted,
                    fieldFilled,
                    ctaClicked,
                    loginStarted,
                    loginCompleted,
                    paywallViewed,
                    checkoutStarted,
                    subscriptionApproved,
                    resolveAbandonmentPoint(),
                    lastEventType,
                    lastActionName,
                    List.copyOf(steps));
        }

        /** Classifica o ponto de abandono mais útil para decisão de marketing. */
        private String resolveAbandonmentPoint() {
            if (subscriptionApproved) {
                return "ASSINATURA_APROVADA";
            }
            if (checkoutStarted) {
                return "ABANDONOU_CHECKOUT";
            }
            if (paywallViewed) {
                return "ABANDONOU_PAYWALL";
            }
            if (loginCompleted) {
                return "ENTROU_SEM_CHEGAR_AO_PAYWALL";
            }
            if (loginStarted) {
                return "ABANDONOU_APOS_SOLICITAR_ACESSO";
            }
            if (fieldInputStarted || fieldFilled) {
                return "ABANDONOU_NO_CAMPO_EMAIL";
            }
            if (fieldFocused) {
                return "FOCOU_EMAIL_SEM_ENVIAR";
            }
            if (ctaClicked) {
                return "CLICOU_CTA_SEM_LOGIN";
            }
            if (maxScrollDepthPercent >= 50 || sectionIds.size() > 1) {
                return "CONSUMIU_PAGINA_SEM_ACAO";
            }
            return "SAIU_NA_PRIMEIRA_DOBRA";
        }

        /** Identifica cliques em CTA sem depender de texto exato do botão. */
        private boolean isCtaClick(SessionJourneyEvent event) {
            if (!"UI_CLICK".equals(event.eventType()) && !"LINK_CLICK".equals(event.eventType())) {
                return false;
            }
            String actionName = event.actionName() == null ? "" : event.actionName().toLowerCase();
            String elementText = event.elementText() == null ? "" : event.elementText().toLowerCase();
            return actionName.contains("cta")
                    || elementText.contains("acesso")
                    || elementText.contains("diagnóstico")
                    || elementText.contains("diagnostico")
                    || elementText.contains("liberar")
                    || elementText.contains("começar")
                    || elementText.contains("comecar");
        }

        /** Adiciona texto não vazio preservando ordem de descoberta. */
        private void addIfPresent(Set<String> values, String value) {
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
    }

    /** Representa o formato persistido do acesso para armazenamento em JSON. */
    private record StoredAccessGrant(
            String productSlug,
            String email,
            String source,
            String createdAt,
            List<String> completedMissionIds,
            Map<String, Map<String, String>> missionInteractions) {

        /** Converte o acesso em memoria para o formato persistido. */
        private static StoredAccessGrant from(AccessGrant grant) {
            return new StoredAccessGrant(
                    grant.getProductSlug(),
                    grant.getEmail(),
                    grant.getSource(),
                    grant.getCreatedAt().toString(),
                    grant.getCompletedMissionIds().stream().toList(),
                    grant.getMissionInteractions());
        }

        /** Reconstrói o acesso de memoria a partir do JSON salvo. */
        private AccessGrant toAccessGrant(String token) {
            return new AccessGrant(
                    token,
                    productSlug,
                    email,
                    source,
                    Instant.parse(createdAt),
                    completedMissionIds != null ? Set.copyOf(completedMissionIds) : Set.of(),
                    missionInteractions);
        }
    }

    /** Acumula dados lidos do banco antes de reconstruir o acesso em memória. */
    private record StoredAccessGrantBuilder(
            String productSlug,
            String email,
            String source,
            Instant createdAt,
            Set<String> completedMissionIds,
            Map<String, Map<String, String>> missionInteractions) {

        /** Cria o acumulador com conjunto mutável de missões concluídas. */
        private StoredAccessGrantBuilder(String productSlug, String email, String source, Instant createdAt) {
            this(productSlug, email, source, createdAt, ConcurrentHashMap.newKeySet(), new LinkedHashMap<>());
        }

        /** Reconstrói o acesso em memória a partir das linhas do banco. */
        private AccessGrant toAccessGrant(String token) {
            return new AccessGrant(token, productSlug, email, source, createdAt, completedMissionIds, missionInteractions);
        }
    }
}
