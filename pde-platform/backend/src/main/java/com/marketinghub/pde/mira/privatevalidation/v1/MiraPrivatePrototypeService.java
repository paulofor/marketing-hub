package com.marketinghub.pde.mira.privatevalidation.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.dto.FunnelEventRequest;
import com.marketinghub.pde.service.AccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Responsabilidade: governar o protótipo privado, retomável e auditável de Mira. */
@Service
public class MiraPrivatePrototypeService {
    private static final Logger log = LoggerFactory.getLogger(MiraPrivatePrototypeService.class);
    private static final String PRODUCT_SLUG = "mira-private-validation";
    private static final String VERSION = "mira-private-v1";
    private static final Set<String> ALLOWED_EVENTS = Set.of("READY_RESULT_USED", "PREFERRED_OVER_FREE", "CHECKOUT_STARTED");
    private final AccessService accessService;
    private final ObjectMapper json;
    private final Path storagePath;
    private final Map<String, String> participantByAccessHash;
    private final Map<String, StoredSession> sessions = new LinkedHashMap<>();

    /** Configura persistência, acessos segregados e trilha de eventos do motor PDE. */
    public MiraPrivatePrototypeService(
            AccessService accessService,
            ObjectMapper json,
            @Value("${pde.mira-private.storage-path}") String storagePath,
            @Value("${pde.mira-private.participant-one-token:}") String participantOneToken,
            @Value("${pde.mira-private.participant-two-token:}") String participantTwoToken,
            @Value("${pde.mira-private.qa-token:}") String qaToken) {
        this.accessService = accessService;
        this.json = json;
        this.storagePath = Path.of(storagePath);
        this.participantByAccessHash = configuredAccesses(participantOneToken, participantTwoToken, qaToken);
        load();
    }

    /** Retorna identidade, eventos e fronteiras verificáveis do protótipo. */
    public ContractResponse contract() {
        return new ContractResponse(PRODUCT_SLUG, VERSION, "Mira", "PLANNED", "SIMULATED_NO_CHARGE",
                List.of("EXPERIENCE_STARTED", "VALUE_MOMENT", "READY_RESULT_USED", "PREFERRED_OVER_FREE", "CHECKOUT_STARTED"),
                false, false, 0);
    }

    /** Autoriza somente um dos dois acessos privados ou o acesso interno de QA. */
    public synchronized SessionResponse access(AccessRequest request) {
        if (!request.consentAccepted()) {
            throw new SecurityException("O consentimento é obrigatório antes de iniciar a sessão privada.");
        }
        String accessHash = hash(request.accessToken());
        String participantReference = participantByAccessHash.get(accessHash);
        if (participantReference == null) {
            throw new SecurityException("Acesso privado inválido.");
        }
        StoredSession session = sessions.values().stream()
                .filter(value -> value.participantReference.equals(participantReference))
                .findFirst()
                .orElseGet(() -> new StoredSession(UUID.randomUUID().toString(), participantReference,
                        participantReference.startsWith("QA-") ? "QA_INTERNAL" : "PRIVATE_READING"));
        sessions.put(session.sessionToken, session);
        if (session.consentedAt == null) session.consentedAt = Instant.now().toString();
        recordOnce(session, "EXPERIENCE_STARTED");
        persist();
        return response(session);
    }

    /** Recupera o estado persistido da sessão autorizada. */
    public synchronized SessionResponse session(String sessionToken) {
        return response(requiredSession(sessionToken));
    }

    /** Salva a entrada e aceita sua repetição idêntica sem reutilizar sinais para outro conteúdo. */
    public synchronized SessionResponse saveInput(String sessionToken, InputRequest request) {
        StoredSession session = requiredSession(sessionToken);
        List<ProductInput> products = request.products().stream()
                .map(product -> new ProductInput(product.name().trim(), product.labelDirections().trim()))
                .toList();
        if (session.finishedAt != null || session.events.contains("VALUE_MOMENT")) {
            if (java.util.Objects.equals(session.ageRange, request.ageRange())
                    && java.util.Objects.equals(session.objective, request.objective().trim())
                    && java.util.Objects.equals(session.products, products)) return response(session);
            throw new IllegalStateException("Esta leitura já possui resultado. Preserve a evidência e use outro convite para uma nova leitura.");
        }
        session.ageRange = request.ageRange();
        session.objective = request.objective().trim();
        session.products = products;
        session.status = "INPUT_READY";
        session.blocker = null;
        session.routine = List.of();
        persist();
        return response(session);
    }

    /** Ordena apenas instruções documentadas e bloqueia qualquer lacuna clínica ou factual. */
    public synchronized SessionResponse generate(String sessionToken) {
        StoredSession session = requiredSession(sessionToken);
        if ("READY".equals(session.status)) return response(session);
        if (session.finishedAt != null) throw new IllegalStateException("A leitura foi encerrada; sua evidência foi preservada.");
        if (session.products == null || session.products.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos um produto e a orientação documentada do rótulo.");
        }
        String objective = normalized(session.objective);
        if (List.of("diagnost", "trat", "cur", "prescre", "doenca", "doença").stream().anyMatch(objective::contains)) {
            return blocked(session, "O objetivo pede conclusão clínica. Reformule como organização de autocuidado ou procure avaliação profissional.");
        }
        List<RoutineCard> cards = new ArrayList<>();
        for (ProductInput product : session.products) {
            Integer order = documentedOrder(product.labelDirections());
            if (order == null) {
                return blocked(session, "Falta orientação documental suficiente para ordenar " + product.name() + ". Informe o rótulo ou fabricante.");
            }
            cards.add(new RoutineCard(product.name(), order, product.labelDirections(), "Ordem limitada ao texto documentado informado; não é prescrição."));
        }
        cards.sort(Comparator.comparingInt(RoutineCard::order));
        session.routine = List.copyOf(cards);
        session.status = "READY";
        session.blocker = null;
        recordOnce(session, "VALUE_MOMENT");
        persist();
        return response(session);
    }

    /** Registra somente ações humanas previstas, idempotentes e sem efeito financeiro. */
    public synchronized SessionResponse event(String sessionToken, EventRequest request) {
        StoredSession session = requiredSession(sessionToken);
        String eventType = request.eventType().trim().toUpperCase();
        if (!ALLOWED_EVENTS.contains(eventType)) {
            throw new IllegalArgumentException("Evento não permitido no protótipo privado de Mira.");
        }
        if (!"READY".equals(session.status)) {
            throw new IllegalStateException("O resultado precisa estar pronto antes desta ação.");
        }
        if (session.finishedAt != null) {
            if (session.events.contains(eventType)) return response(session);
            throw new IllegalStateException("A leitura já foi encerrada; suas respostas foram preservadas.");
        }
        if ("PREFERRED_OVER_FREE".equals(eventType) && !session.events.contains("READY_RESULT_USED")) {
            throw new IllegalStateException("Consulte o resultado antes de registrar sua preferência.");
        }
        if ("PREFERRED_OVER_FREE".equals(eventType) && !Boolean.TRUE.equals(request.confirmed())) {
            throw new IllegalArgumentException("A preferência deve refletir uma escolha humana explícita.");
        }
        if ("CHECKOUT_STARTED".equals(eventType) && !session.events.contains("PREFERRED_OVER_FREE")) {
            throw new IllegalStateException("A simulação só fica disponível após a preferência explícita.");
        }
        recordOnce(session, eventType);
        if ("CHECKOUT_STARTED".equals(eventType)) session.finishedAt = Instant.now().toString();
        persist();
        return response(session);
    }

    /** Encerra também uma leitura negativa sem fabricar preferência ou intenção de compra. */
    public synchronized SessionResponse finish(String sessionToken) {
        StoredSession session = requiredSession(sessionToken);
        if (session.finishedAt == null) {
            session.finishedAt = Instant.now().toString();
            persist();
        }
        return response(session);
    }

    /** Expõe somente prova sanitizada da leitura solicitada, sem acesso, sessão ou dados de entrada. */
    public synchronized ReadingEvidence readingEvidence(int readingNumber) {
        if (readingNumber != 1 && readingNumber != 2) {
            throw new IllegalArgumentException("Informe primeira ou segunda leitura privada.");
        }
        String participant = "PV-00000000000" + readingNumber;
        StoredSession session = sessions.values().stream()
                .filter(value -> participant.equals(value.participantReference))
                .filter(value -> "PRIVATE_READING".equals(value.trafficClass))
                .findFirst().orElse(null);
        Map<String, Boolean> signals = new LinkedHashMap<>();
        contract().instrumentationEvents().forEach(event -> signals.put(event, session != null && session.events.contains(event)));
        return new ReadingEvidence(PRODUCT_SLUG, VERSION, participant,
                session == null ? "NOT_STARTED" : session.trafficClass,
                session == null ? null : session.evidenceId,
                session == null ? null : session.consentedAt,
                session == null ? null : session.finishedAt,
                session == null ? null : session.blocker, Map.copyOf(signals),
                "SIMULATED_NO_CHARGE", false, false, 0);
    }

    /** Persiste um bloqueio explicável sem inventar uma rotina. */
    private SessionResponse blocked(StoredSession session, String blocker) {
        session.status = "BLOCKED";
        session.blocker = blocker;
        session.routine = List.of();
        persist();
        return response(session);
    }

    /** Classifica somente usos explicitamente descritos no rótulo informado. */
    private Integer documentedOrder(String directions) {
        String value = normalized(directions);
        if (value.contains("apos a limpeza") || value.contains("hidrat")) return 20;
        if (value.contains("limpar") || value.contains("enxagu")) return 10;
        if (value.contains("protetor") || value.contains("protecao solar") || value.contains("proteção solar")) return 30;
        return null;
    }

    /** Registra cada evento apenas uma vez por sessão e por contrato. */
    private void recordOnce(StoredSession session, String eventType) {
        if (session.events.contains(eventType)) return;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("participantReference", session.participantReference);
        metadata.put("sessionId", session.evidenceId);
        metadata.put("experienceVersion", VERSION);
        metadata.put("trafficClass", session.trafficClass);
        metadata.put("checkoutMode", "SIMULATED_NO_CHARGE");
        metadata.put("paymentEnabled", false);
        metadata.put("published", false);
        metadata.put("mediaSpendBrl", 0);
        accessService.recordFunnelEvent(new FunnelEventRequest(
                PRODUCT_SLUG, eventType, null, null, null, "mira-private-prototype", null, metadata));
        session.events.add(eventType);
    }

    /** Exige uma sessão emitida pelo backend sem aceitar identificador escolhido pelo cliente. */
    private StoredSession requiredSession(String sessionToken) {
        StoredSession session = sessions.get(sessionToken == null ? "" : sessionToken.trim());
        if (session == null) throw new SecurityException("Sessão privada inválida ou expirada.");
        return session;
    }

    /** Monta a projeção sanitizada utilizada pela interface. */
    private SessionResponse response(StoredSession session) {
        return new SessionResponse(session.sessionToken, session.participantReference, session.trafficClass,
                session.status, session.ageRange, session.objective, session.products == null ? List.of() : session.products,
                session.routine == null ? List.of() : session.routine, session.blocker,
                List.copyOf(session.events), VERSION, "SIMULATED_NO_CHARGE", session.finishedAt != null);
    }

    /** Cria hashes dos acessos sem armazenar segredos brutos. */
    private Map<String, String> configuredAccesses(String first, String second, String qa) {
        Map<String, String> values = new LinkedHashMap<>();
        if (first != null && !first.isBlank()) values.put(hash(first), "PV-000000000001");
        if (second != null && !second.isBlank()) values.put(hash(second), "PV-000000000002");
        if (qa != null && !qa.isBlank()) values.put(hash(qa), "QA-MIRA-LOCAL");
        return Map.copyOf(values);
    }

    /** Calcula identidade irreversível para comparar acessos configurados. */
    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            log.error("Falha ao calcular hash de acesso privado de Mira", ex);
            throw new IllegalStateException("Não foi possível validar o acesso privado.", ex);
        }
    }

    /** Normaliza texto exclusivamente para gates determinísticos de segurança. */
    private String normalized(String value) {
        return value == null ? "" : java.text.Normalizer.normalize(value.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    /** Carrega checkpoints persistidos sem impedir a inicialização quando o arquivo ainda não existe. */
    private void load() {
        if (!Files.exists(storagePath)) return;
        try {
            sessions.putAll(json.readValue(storagePath.toFile(), new TypeReference<>() {}));
        } catch (Exception ex) {
            log.error("Falha ao carregar sessões privadas de Mira; storagePath={}", storagePath, ex);
            throw new IllegalStateException("Não foi possível carregar as sessões privadas de Mira.", ex);
        }
    }

    /** Grava checkpoints atomicamente para preservar retomada após reinício. */
    private void persist() {
        try {
            Files.createDirectories(storagePath.getParent());
            Path temporary = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
            json.writeValue(temporary.toFile(), sessions);
            Files.move(temporary, storagePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            log.error("Falha ao persistir sessões privadas de Mira; storagePath={}", storagePath, ex);
            throw new IllegalStateException("Não foi possível preservar a sessão privada.", ex);
        }
    }

    /** Entrada para trocar o acesso opaco por sessão autorizada. */
    public record AccessRequest(@NotBlank @Size(max = 512) String accessToken, boolean consentAccepted) {}

    /** Produto e texto documental mínimo informado pela pessoa. */
    public record ProductInput(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 600) String labelDirections) {}

    /** Entrada segura, limitada e não clínica do protótipo. */
    public record InputRequest(@NotBlank @Size(max = 40) String ageRange,
                               @NotBlank @Size(max = 500) String objective,
                               @NotEmpty @Size(max = 12) List<@Valid ProductInput> products) {}

    /** Ação humana prevista pelo protocolo privado. */
    public record EventRequest(@NotBlank String eventType, Boolean confirmed) {}

    /** Cartão funcional derivado apenas da informação documentada. */
    public record RoutineCard(String productName, int order, String documentedDirection, String safetyNote) {}

    /** Contrato sanitizado da experiência privada. */
    public record ContractResponse(String productSlug, String prototypeVersion, String productInternalName,
                                   String productStatus, String checkoutMode, List<String> instrumentationEvents,
                                   boolean published, boolean paymentEnabled, int mediaSpendBrl) {}

    /** Estado retomável enviado à interface sem expor hashes ou outros participantes. */
    public record SessionResponse(String sessionToken, String participantReference, String trafficClass,
                                  String status, String ageRange, String objective, List<ProductInput> products,
                                  List<RoutineCard> routine, String blocker, List<String> events,
                                  String prototypeVersion, String checkoutMode, boolean readingFinished) {}

    /** Prova interna pseudonimizada, independente das credenciais que autorizam a sessão. */
    public record ReadingEvidence(String productSlug, String prototypeVersion, String participantReference,
                                   String trafficClass, String evidenceId, String consentedAt, String finishedAt,
                                   String blocker, Map<String, Boolean> signals, String checkoutMode,
                                   boolean paymentEnabled, boolean published, int mediaSpendBrl) {}

    /** Estado interno persistido de uma única sessão segregada. */
    public static final class StoredSession {
        public String sessionToken;
        public String participantReference;
        public String trafficClass;
        public String evidenceId = UUID.randomUUID().toString();
        public String consentedAt;
        public String finishedAt;
        public String status = "CONSENTED";
        public String ageRange;
        public String objective;
        public List<ProductInput> products = List.of();
        public List<RoutineCard> routine = List.of();
        public String blocker;
        public Set<String> events = new java.util.LinkedHashSet<>();

        /** Construtor vazio utilizado exclusivamente pela persistência JSON. */
        public StoredSession() {}

        /** Cria uma nova sessão vinculada ao participante e à classe de tráfego. */
        StoredSession(String sessionToken, String participantReference, String trafficClass) {
            this.sessionToken = sessionToken;
            this.participantReference = participantReference;
            this.trafficClass = trafficClass;
        }
    }
}
