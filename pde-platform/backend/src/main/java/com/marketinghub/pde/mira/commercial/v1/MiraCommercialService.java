package com.marketinghub.pde.mira.commercial.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.dto.PrivacyActionRequest;
import com.marketinghub.pde.dto.PrivacyActionResponse;
import com.marketinghub.pde.mira.common.v1.MiraRoutinePolicy;
import com.marketinghub.pde.service.AccessService;
import com.marketinghub.pde.service.RigelPaidEntitlementService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Responsabilidade: entregar a rotina paga, retomável e auditável de Mira. */
@Service
public class MiraCommercialService {
    public static final String PRODUCT_SLUG = RigelPaidEntitlementService.MIRA_PRODUCT_SLUG;
    public static final String EXPERIENCE_VERSION =
            RigelPaidEntitlementService.MIRA_EXPERIENCE_VERSION;
    private static final Logger log = LoggerFactory.getLogger(MiraCommercialService.class);
    private static final int MAX_ATTEMPTS = 2;
    private static final String SOURCE = "pde-mira-commercial";
    private final AccessService accessService;
    private final ObjectMapper json;
    private final Path storagePath;
    private final Map<String, StoredSession> sessions = new LinkedHashMap<>();

    /** Configura acesso pago e armazenamento retomável segregado da pesquisa privada. */
    public MiraCommercialService(
            AccessService accessService,
            ObjectMapper json,
            @Value("${pde.mira-commercial.storage-path:/data/pde/mira-commercial-sessions.json}")
                    String storagePath) {
        this.accessService = accessService;
        this.json = json;
        this.storagePath = Path.of(storagePath);
        load();
    }

    /** Expõe identidade, preço, limites e eventos verificáveis da entrega comercial. */
    public ContractResponse contract() {
        return new ContractResponse(
                PRODUCT_SLUG,
                EXPERIENCE_VERSION,
                "Sua rotina de cuidados, organizada com o que você já tem",
                49,
                "BRL",
                "ONE_TIME",
                MAX_ATTEMPTS,
                List.of(
                        "PAGE_VIEW",
                        "CTA_VIEWED",
                        "VIDEO_PLAY",
                        "VIDEO_COMPLETED",
                        "CHECKOUT_STARTED",
                        "VALUE_MOMENT",
                        "PURCHASE_COMPLETED",
                        "ACCESS_RELEASED",
                        "FIRST_USE",
                        "JOURNEY_COMPLETED",
                        "REFUND_CONFIRMED"),
                false,
                false);
    }

    /** Recupera ou inicia a entrega somente para um bearer pago e vigente de Mira. */
    public synchronized SessionResponse session(String accessToken) {
        AccessService.PaidAccessIdentity identity = authorize(accessToken);
        String accessReference = hash(accessToken);
        StoredSession session = sessions.computeIfAbsent(
                accessReference,
                ignored -> new StoredSession(
                        accessReference,
                        identity.experienceVersion(),
                        "STARTED",
                        Instant.now().toString()));
        persist();
        return response(session);
    }

    /** Salva uma tentativa sem aceitar alteração depois que as duas entregas foram consumidas. */
    public synchronized SessionResponse saveInput(String accessToken, InputRequest request) {
        authorize(accessToken);
        StoredSession session = requiredSession(accessToken);
        List<ProductInput> products = request.products().stream()
                .map(product -> new ProductInput(
                        product.name().trim(), product.labelDirections().trim()))
                .toList();
        boolean unchanged = java.util.Objects.equals(session.objective, request.objective().trim())
                && java.util.Objects.equals(session.products, products);
        if (unchanged && "READY".equals(session.status)) return response(session);
        if (session.attempts >= MAX_ATTEMPTS) {
            throw new IllegalStateException(
                    "As duas organizações incluídas já foram usadas. Seu último resultado continua disponível.");
        }
        session.objective = request.objective().trim();
        session.products = products;
        session.routine = List.of();
        session.blocker = null;
        session.status = "INPUT_READY";
        persist();
        return response(session);
    }

    /** Gera uma rotina determinística ou bloqueia o pedido sem inventar orientação clínica. */
    public synchronized SessionResponse generate(String accessToken) {
        AccessService.PaidAccessIdentity identity = authorize(accessToken);
        StoredSession session = requiredSession(accessToken);
        if ("READY".equals(session.status)) return response(session);
        if (session.attempts >= MAX_ATTEMPTS) {
            throw new IllegalStateException("O limite de duas organizações desta compra foi atingido.");
        }
        var decision = MiraRoutinePolicy.organize(
                session.objective,
                session.products.stream()
                        .map(product -> new MiraRoutinePolicy.ProductInput(
                                product.name(), product.labelDirections()))
                        .toList());
        if (decision.blocked()) {
            session.status = "BLOCKED";
            session.blocker = decision.blocker();
            session.routine = List.of();
            persist();
            recordOnce(accessToken, identity, session, "SAFETY_LIMIT_BLOCKED");
            return response(session);
        }
        session.routine = decision.routine().stream()
                .map(card -> new RoutineCard(
                        card.productName(),
                        card.order(),
                        card.documentedDirection(),
                        card.safetyNote()))
                .toList();
        session.status = "READY";
        session.blocker = null;
        session.attempts += 1;
        session.generatedAt = Instant.now().toString();
        persist();
        recordOnce(accessToken, identity, session, "VALUE_MOMENT");
        return response(session);
    }

    /** Registra uso e conclusão somente depois que um resultado utilizável existe. */
    public synchronized SessionResponse recordEvent(String accessToken, EventRequest request) {
        AccessService.PaidAccessIdentity identity = authorize(accessToken);
        StoredSession session = requiredSession(accessToken);
        String eventType = request.eventType().trim().toUpperCase();
        if (!List.of("READY_RESULT_USED", "FIRST_USE", "JOURNEY_COMPLETED").contains(eventType)) {
            throw new IllegalArgumentException("Evento comercial de Mira não suportado.");
        }
        if (!"READY".equals(session.status)) {
            throw new IllegalStateException("Consulte uma rotina pronta antes de registrar o uso.");
        }
        if ("JOURNEY_COMPLETED".equals(eventType)
                && !session.events.contains("FIRST_USE")) {
            throw new IllegalStateException("Confirme o primeiro uso antes de concluir a jornada.");
        }
        recordOnce(accessToken, identity, session, eventType);
        if ("JOURNEY_COMPLETED".equals(eventType)) {
            session.completedAt = Instant.now().toString();
            persist();
        }
        return response(session);
    }

    /** Executa privacidade sobre acesso e conteúdo comercial no mesmo comando autenticado. */
    public synchronized PrivacyActionResponse privacy(
            String accessToken, PrivacyActionRequest request) {
        authorize(accessToken);
        String accessReference = hash(accessToken);
        String action = request.action().trim().toUpperCase();
        Map<String, Object> miraData = new LinkedHashMap<>();
        StoredSession session = sessions.get(accessReference);
        if (session != null && "ACCESS".equals(action)) {
            miraData.put("miraCommercial", response(session));
        }
        if ("DELETION".equals(action)) {
            sessions.remove(accessReference);
            persist();
        }
        PrivacyActionResponse generic = accessService.executePrivacyAction(accessToken, request);
        if (miraData.isEmpty()) return generic;
        Map<String, Object> merged = new LinkedHashMap<>(generic.data());
        merged.putAll(miraData);
        return new PrivacyActionResponse(
                generic.action(), generic.status(), generic.executedAt(), Map.copyOf(merged));
    }

    /** Confirma que o token representa a versão comercial paga de Mira. */
    private AccessService.PaidAccessIdentity authorize(String accessToken) {
        AccessService.PaidAccessIdentity identity =
                accessService.requirePaidAccessIdentity(accessToken, PRODUCT_SLUG);
        if (!EXPERIENCE_VERSION.equals(identity.experienceVersion())) {
            throw new SecurityException("O acesso pertence a outra versão de Mira.");
        }
        return identity;
    }

    /** Exige uma sessão já inicializada pelo endpoint de entrada autenticado. */
    private StoredSession requiredSession(String accessToken) {
        StoredSession session = sessions.get(hash(accessToken));
        if (session == null) {
            throw new IllegalStateException("Abra sua área de Mira antes de enviar os produtos.");
        }
        return session;
    }

    /** Registra cada marco uma vez e separa explicitamente o tráfego interno de QA. */
    private void recordOnce(
            String accessToken,
            AccessService.PaidAccessIdentity identity,
            StoredSession session,
            String eventType) {
        if (session.events.contains(eventType)) return;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("experienceVersion", EXPERIENCE_VERSION);
        metadata.put("experimentId", 93);
        metadata.put("accessReferenceHash", session.accessReference);
        metadata.put("attemptsUsed", session.attempts);
        metadata.put("idempotencyKey", eventType.toLowerCase() + ":" + session.accessReference);
        if ("INTERNAL_QA".equalsIgnoreCase(identity.source())) {
            metadata.put("mh_internal_test", true);
            metadata.put("trafficQuality", "INTERNAL_QA");
        }
        accessService.recordFunnelEvent(new FunnelEventRequest(
                PRODUCT_SLUG,
                eventType,
                accessToken,
                identity.email(),
                identity.source(),
                SOURCE,
                "https://mira.digicomdigital.com.br/access",
                metadata));
        session.events.add(eventType);
        persist();
    }

    /** Converte o estado persistido em resposta sem e-mail ou bearer. */
    private SessionResponse response(StoredSession session) {
        return new SessionResponse(
                EXPERIENCE_VERSION,
                session.status,
                session.objective,
                session.products == null ? List.of() : List.copyOf(session.products),
                session.routine == null ? List.of() : List.copyOf(session.routine),
                session.blocker,
                session.attempts,
                MAX_ATTEMPTS,
                List.copyOf(session.events),
                session.generatedAt,
                session.completedAt);
    }

    /** Carrega sessões comerciais sem reutilizar a persistência da pesquisa privada. */
    private void load() {
        if (!Files.exists(storagePath)) return;
        try {
            sessions.putAll(json.readValue(storagePath.toFile(), new TypeReference<>() {}));
        } catch (Exception ex) {
            log.error(
                    "Falha ao carregar sessões comerciais de Mira; storagePath={}", storagePath, ex);
            throw new IllegalStateException("Não foi possível recuperar sua área de Mira.", ex);
        }
    }

    /** Persiste de forma atômica para preservar retomada depois de reinício do container. */
    private void persist() {
        try {
            Path parent = storagePath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Path temporary = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
            json.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), sessions);
            Files.move(
                    temporary,
                    storagePath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            log.error(
                    "Falha ao persistir sessões comerciais de Mira; storagePath={}", storagePath, ex);
            throw new IllegalStateException("Não foi possível salvar sua rotina agora.", ex);
        }
    }

    /** Calcula referência irreversível para segregar a sessão sem persistir o bearer. */
    private String hash(String value) {
        if (value == null || value.isBlank()) throw new SecurityException("Acesso de Mira não informado.");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.trim().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            log.error("Falha ao proteger referência do acesso comercial de Mira", ex);
            throw new IllegalStateException("Não foi possível validar seu acesso.", ex);
        }
    }

    /** Representa a entrada comercial informada pela cliente. */
    public record InputRequest(
            @NotBlank @Size(max = 240) String objective,
            @NotEmpty @Size(max = 12) List<ProductInput> products) {}

    /** Representa um produto já possuído e sua orientação de rótulo. */
    public record ProductInput(
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 600) String labelDirections) {}

    /** Representa um item ordenado da rotina pronta. */
    public record RoutineCard(
            String productName,
            int order,
            String documentedDirection,
            String safetyNote) {}

    /** Representa um marco explícito de uso da rotina. */
    public record EventRequest(@NotBlank String eventType) {}

    /** Expõe o estado retomável sem credenciais ou dados financeiros sensíveis. */
    public record SessionResponse(
            String experienceVersion,
            String status,
            String objective,
            List<ProductInput> products,
            List<RoutineCard> routine,
            String blocker,
            int attemptsUsed,
            int attemptsLimit,
            List<String> events,
            String generatedAt,
            String completedAt) {}

    /** Expõe o contrato comercial e os limites públicos de Mira. */
    public record ContractResponse(
            String productSlug,
            String experienceVersion,
            String promise,
            int priceBrl,
            String currency,
            String billingModel,
            int attemptsLimit,
            List<String> instrumentationEvents,
            boolean subscription,
            boolean recommendsProducts) {}

    /** Representa somente o conteúdo retomável associado ao hash do acesso. */
    public static final class StoredSession {
        public String accessReference;
        public String experienceVersion;
        public String status;
        public String createdAt;
        public String objective;
        public List<ProductInput> products = List.of();
        public List<RoutineCard> routine = List.of();
        public String blocker;
        public int attempts;
        public List<String> events = new java.util.ArrayList<>();
        public String generatedAt;
        public String completedAt;

        /** Permite ao Jackson reconstruir uma sessão persistida. */
        public StoredSession() {}

        /** Cria uma sessão vazia vinculada à versão comercial paga. */
        private StoredSession(
                String accessReference,
                String experienceVersion,
                String status,
                String createdAt) {
            this.accessReference = accessReference;
            this.experienceVersion = experienceVersion;
            this.status = status;
            this.createdAt = createdAt;
        }
    }
}
