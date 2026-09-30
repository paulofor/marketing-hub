package com.marketinghub.pde.agentvalidation.v1;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Responsabilidade: governar sessões e continuidade segura dos protótipos visuais do PDE. */
@Service
public class PdeStaticVisualAgentValidationService {
    private static final Logger log = LoggerFactory.getLogger(PdeStaticVisualAgentValidationService.class);
    private static final long PRODUCT_ID = 11L;
    private static final String PRODUCT_SLUG = "pde-planejado-46";
    private static final String SOURCE_REFERENCE = "product:11@agent-validation-v1";
    private static final String VERSION = "alcyone-private-v2";
    private static final String PREVIOUS_VERSION = "alcyone-private-v1";
    private static final String FIXTURE_CONTRACT = "PDE_STATIC_RESULT_FIXTURES_V1";
    private static final String CONTINUITY_POLICY_VERSION = "ALCYONE_AGENT_CONTINUITY_V1";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final Duration CONTINUITY_TTL = Duration.ofHours(24);
    private static final Set<String> SCENARIOS = Set.of("ADHERENT", "RECOVERY", "SAFETY");
    private static final Set<String> EVENTS = Set.of(
            "VALUE_MOMENT",
            "READY_RESULT_USED",
            "SAVE_INTEREST_DECLARED",
            "RETURN_COMPLETED",
            "PREFERRED_OVER_FREE",
            "CHECKOUT_STARTED",
            "RECOVERY_COMPLETED",
            "SAFETY_LIMIT_BLOCKED",
            "AGENT_SCENARIO_COMPLETED");
    private static final Set<String> CONTINUITY_EVENTS = Set.of(
            "SAVE_INTEREST_DECLARED",
            "CONTINUITY_POLICY_ACKNOWLEDGED",
            "CONTINUITY_CREDENTIAL_CREATED",
            "AUTHENTICATED_ACCESS_RESTORED",
            "RETURN_COMPLETED");
    private static final List<LookCard> LOOKS = List.of(
            new LookCard(
                    "look-1",
                    "Azul profundo com alfaiataria clara",
                    "/assets/alcyone/look-1.png",
                    "Equilibra formalidade e leveza com linhas simples e contraste controlado.",
                    "Use o blazer aberto e mantenha os acessórios discretos."),
            new LookCard(
                    "look-2",
                    "Monocromático frio com textura suave",
                    "/assets/alcyone/look-2.png",
                    "Reduz a dúvida entre peças e preserva conforto para uma ocasião longa.",
                    "Dobre levemente a manga para adaptar a variações de temperatura."),
            new LookCard(
                    "look-3",
                    "Grafite com ponto de cor azul",
                    "/assets/alcyone/look-3.png",
                    "Mantém uma base versátil e cria presença sem depender de uma peça nova.",
                    "Escolha o calçado baixo já disponível e repita apenas um tom de destaque."));

    private final ObjectMapper json;
    private final Path storagePath;
    private final Clock clock;
    private final Map<String, StoredSession> sessions = new LinkedHashMap<>();

    /** Configura a persistência atômica e o relógio real do motor compartilhado. */
    @Autowired
    public PdeStaticVisualAgentValidationService(
            ObjectMapper json,
            @Value("${pde.agent-validation.static-visual-storage-path}") String storagePath) {
        this(json, Path.of(storagePath), Clock.systemUTC());
    }

    /** Permite testar expiração e retomada com relógio determinístico. */
    PdeStaticVisualAgentValidationService(ObjectMapper json, Path storagePath, Clock clock) {
        this.json = json;
        this.storagePath = storagePath;
        this.clock = clock;
        load();
    }

    /** Expõe a identidade imutável e as travas comerciais da superfície Alcyone. */
    public ContractResponse contract(String productSlug) {
        requireProduct(productSlug);
        return new ContractResponse(
                PRODUCT_ID,
                PRODUCT_SLUG,
                VERSION,
                "Três combinações para uma ocasião específica",
                FIXTURE_CONTRACT,
                "/assets/alcyone/manifest.json",
                "PLANNED",
                "SIMULATED_NO_CHARGE",
                CONTINUITY_POLICY_VERSION,
                SESSION_TTL.toSeconds(),
                CONTINUITY_TTL.toSeconds(),
                List.of(
                        "EXPERIENCE_STARTED",
                        "VALUE_MOMENT",
                        "READY_RESULT_USED",
                        "SAVE_INTEREST_DECLARED",
                        "CONTINUITY_POLICY_ACKNOWLEDGED",
                        "CONTINUITY_CREDENTIAL_CREATED",
                        "AUTHENTICATED_ACCESS_RESTORED",
                        "RETURN_COMPLETED",
                        "PREFERRED_OVER_FREE",
                        "CHECKOUT_STARTED"),
                false,
                false,
                0,
                0);
    }

    /** Cria uma sessão pseudonimizada exclusiva para um cenário e uma execução de agente. */
    public synchronized SessionResponse startAgentValidation(
            String productSlug, AgentSessionRequest request) {
        requireProduct(productSlug);
        String scenarioCode = request.scenarioCode().trim().toUpperCase();
        if (!SCENARIOS.contains(scenarioCode)) {
            throw new IllegalArgumentException("Cenário de homologação multiagente inválido.");
        }
        if (!SOURCE_REFERENCE.equals(request.sourceReference().trim())) {
            throw new IllegalArgumentException("A sessão não corresponde ao produto Alcyone canônico.");
        }
        String sessionToken = credential();
        StoredSession session = new StoredSession();
        session.prototypeVersion = VERSION;
        session.sessionTokenHash = hash(sessionToken);
        session.sessionExpiresAt = now().plus(SESSION_TTL).toString();
        session.evidenceId = UUID.randomUUID().toString();
        session.scenarioCode = scenarioCode;
        session.sourceReference = SOURCE_REFERENCE;
        session.readingId = UUID.randomUUID().toString();
        session.executionId = UUID.randomUUID().toString();
        sessions.put(session.sessionTokenHash, session);
        persist();
        return response(session, sessionToken);
    }

    /** Recupera o checkpoint de uma sessão ainda válida sem renovar sua expiração. */
    public synchronized SessionResponse session(String productSlug, String sessionToken) {
        requireProduct(productSlug);
        return response(requiredSession(sessionToken), null);
    }

    /** Persiste a entrada mínima e registra início somente depois de aceitá-la. */
    public synchronized SessionResponse saveInput(
            String productSlug, String sessionToken, InputRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        InputSnapshot snapshot = new InputSnapshot(
                request.occasion().trim(),
                request.eventDate(),
                trimmed(request.preferences()),
                trimmed(request.constraints()),
                trimmed(request.pieceReferences()));
        if (session.finished || session.events.contains("VALUE_MOMENT")) {
            if (snapshot.equals(session.input)) return response(session, null);
            throw new IllegalStateException("A execução já possui evidência e não pode receber outra entrada.");
        }
        session.input = snapshot;
        session.status = "INPUT_READY";
        session.blocker = null;
        session.looks = List.of();
        recordOnce(session, "EXPERIENCE_STARTED");
        persist();
        return response(session, null);
    }

    /** Entrega as três fixtures determinísticas ou bloqueia uma entrada fora do escopo seguro. */
    public synchronized SessionResponse generate(String productSlug, String sessionToken) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        if (session.finished) {
            throw new IllegalStateException("O cenário foi encerrado e sua evidência está preservada.");
        }
        if ("READY".equals(session.status) || "BLOCKED".equals(session.status)) {
            return response(session, null);
        }
        if (session.input == null || !session.events.contains("EXPERIENCE_STARTED")) {
            throw new IllegalStateException("A entrada mínima precisa ser aceita antes do resultado.");
        }
        if (unsafe(session.input)) {
            session.status = "BLOCKED";
            session.blocker =
                    "Esta versão aceita somente referências isoladas das peças e não recebe foto corporal, "
                            + "recomendação de compra ou julgamento do corpo.";
            session.looks = List.of();
            persist();
            return response(session, null);
        }
        session.looks = LOOKS;
        session.status = "READY";
        session.blocker = null;
        session.resultPackageId = UUID.randomUUID().toString();
        session.resultPackageFingerprint =
                "alcyone-static-fixtures-v1:3x1024x1024:provider-calls-0";
        persist();
        return response(session, null);
    }

    /** Registra apenas sinais explícitos na sequência funcional do cenário sintético. */
    public synchronized SessionResponse event(
            String productSlug, String sessionToken, EventRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        String eventType = request.eventType().trim().toUpperCase();
        if (!EVENTS.contains(eventType)) {
            throw new IllegalArgumentException("Evento não permitido no protótipo multiagente.");
        }
        if (session.finished) {
            if (session.events.contains(eventType)) return response(session, null);
            throw new IllegalStateException("O cenário já foi encerrado.");
        }
        validateEvent(session, eventType, request);
        recordOnce(session, eventType);
        if ("READY_RESULT_USED".equals(eventType)) {
            session.selectedLookId = request.selectedLookId().trim();
        }
        if ("AGENT_SCENARIO_COMPLETED".equals(eventType)) session.finished = true;
        persist();
        return response(session, null);
    }

    /** Emite uma credencial de continuidade somente após interesse e política explícitos. */
    public synchronized ContinuityCredentialResponse createContinuity(
            String productSlug, String sessionToken, ContinuityCredentialRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        validatePolicy(request.policyAcknowledged(), request.policyVersion());
        if (session.finished
                || !"READY".equals(session.status)
                || session.resultPackageId == null
                || !session.events.contains("SAVE_INTEREST_DECLARED")) {
            throw new IllegalStateException(
                    "A continuidade exige resultado usado e interesse de retorno declarado.");
        }
        String continuationCredential = credential();
        session.continuityPolicyVersion = CONTINUITY_POLICY_VERSION;
        session.policyAcknowledgedAt = now().toString();
        session.continuationCredentialHash = hash(continuationCredential);
        session.continuityExpiresAt = now().plus(CONTINUITY_TTL).toString();
        recordOnce(session, "CONTINUITY_POLICY_ACKNOWLEDGED");
        recordOnce(session, "CONTINUITY_CREDENTIAL_CREATED");
        persist();
        return new ContinuityCredentialResponse(
                CONTINUITY_POLICY_VERSION,
                session.policyAcknowledgedAt,
                session.continuityExpiresAt,
                session.resultPackageId,
                continuationCredential);
    }

    /** Restaura acesso, rotaciona as duas credenciais e mantém o mesmo pacote do cenário. */
    public synchronized ContinuityResumeResponse resumeContinuity(
            String productSlug, ContinuityResumeRequest request) {
        requireProduct(productSlug);
        if (!CONTINUITY_POLICY_VERSION.equals(trim(request.policyVersion()))) {
            throw new SecurityException("A política da credencial de continuidade não corresponde à versão ativa.");
        }
        String credentialHash = hash(request.continuationCredential());
        StoredSession session = sessions.values().stream()
                .filter(value -> credentialHash.equals(value.continuationCredentialHash))
                .findFirst()
                .orElseThrow(() -> new SecurityException("Credencial de continuidade inválida ou expirada."));
        if (!VERSION.equals(session.prototypeVersion)
                || session.continuityExpiresAt == null
                || !now().isBefore(Instant.parse(session.continuityExpiresAt))
                || !CONTINUITY_POLICY_VERSION.equals(session.continuityPolicyVersion)) {
            throw new SecurityException("Credencial de continuidade inválida ou expirada.");
        }
        String previousSessionHash = session.sessionTokenHash;
        String newSessionToken = credential();
        String newContinuationCredential = credential();
        session.sessionTokenHash = hash(newSessionToken);
        session.sessionExpiresAt = now().plus(SESSION_TTL).toString();
        session.continuationCredentialHash = hash(newContinuationCredential);
        session.continuityExpiresAt = now().plus(CONTINUITY_TTL).toString();
        if (previousSessionHash != null) sessions.remove(previousSessionHash);
        sessions.put(session.sessionTokenHash, session);
        recordOnce(session, "AUTHENTICATED_ACCESS_RESTORED");
        persist();
        return new ContinuityResumeResponse(
                newSessionToken,
                session.sessionExpiresAt,
                newContinuationCredential,
                session.continuityExpiresAt,
                session.resultPackageId,
                response(session, null));
    }

    /** Entrega o pacote somente à sessão ativa que o originou. */
    public synchronized ResultPackageResponse resultPackage(
            String productSlug, String sessionToken, String packageId) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        if (session.resultPackageId == null
                || !session.resultPackageId.equals(trim(packageId))
                || !"READY".equals(session.status)) {
            throw new SecurityException("O pacote não pertence à sessão autenticada.");
        }
        return new ResultPackageResponse(
                session.resultPackageId,
                session.resultPackageFingerprint,
                List.copyOf(session.looks));
    }

    /** Expira uma sessão pelo harness para comprovar a retomada sem esperar o relógio real. */
    public synchronized Map<String, Object> expireSessionForHarness(
            String productSlug, InternalSessionExpirationRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(request.sessionToken());
        session.sessionExpiresAt = now().minusSeconds(1).toString();
        persist();
        return Map.of("expired", true, "evidenceId", session.evidenceId);
    }

    /** Entrega evidência sintética sanitizada sem credencial ou alegação humana. */
    public synchronized AgentValidationEvidence evidence(String productSlug, String evidenceId) {
        requireProduct(productSlug);
        StoredSession session = sessions.values().stream()
                .filter(value -> value.evidenceId.equals(trim(evidenceId)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Evidência multiagente não encontrada."));
        return new AgentValidationEvidence(
                PRODUCT_ID,
                PRODUCT_SLUG,
                session.prototypeVersion,
                session.sourceReference,
                session.scenarioCode,
                "AGENT_VALIDATION",
                true,
                session.evidenceId,
                session.readingId,
                session.executionId,
                session.status,
                session.input,
                List.copyOf(session.looks),
                session.selectedLookId,
                session.blocker,
                List.copyOf(session.events),
                session.finished,
                session.resultPackageId,
                session.resultPackageFingerprint,
                FIXTURE_CONTRACT,
                session.continuityPolicyVersion,
                session.policyAcknowledgedAt,
                session.sessionExpiresAt,
                session.continuityExpiresAt,
                session.continuationCredentialHash != null,
                List.copyOf(eventAudit(session)),
                0,
                0,
                Map.of(
                        "paymentEnabled", false,
                        "published", false,
                        "campaignCreated", false,
                        "mediaSpendBrl", 0),
                false,
                false);
    }

    /** Valida a dependência funcional de cada evento sem inferir comportamento não observado. */
    private void validateEvent(StoredSession session, String eventType, EventRequest request) {
        if ("SAFETY_LIMIT_BLOCKED".equals(eventType)) {
            if (!"SAFETY".equals(session.scenarioCode) || !"BLOCKED".equals(session.status)) {
                throw new IllegalStateException("O limite de segurança precisa estar bloqueado no cenário correto.");
            }
            return;
        }
        if ("AGENT_SCENARIO_COMPLETED".equals(eventType)) {
            boolean adherent = "ADHERENT".equals(session.scenarioCode)
                    && canonicalFunnelComplete(session)
                    && session.events.containsAll(CONTINUITY_EVENTS);
            boolean recovery = "RECOVERY".equals(session.scenarioCode)
                    && canonicalFunnelComplete(session)
                    && session.events.containsAll(CONTINUITY_EVENTS)
                    && session.events.contains("RECOVERY_COMPLETED");
            boolean safety = "SAFETY".equals(session.scenarioCode)
                    && "BLOCKED".equals(session.status)
                    && session.events.contains("SAFETY_LIMIT_BLOCKED");
            if (!adherent && !recovery && !safety) {
                throw new IllegalStateException("O cenário não possui toda a evidência funcional exigida.");
            }
            return;
        }
        if (!"READY".equals(session.status)) {
            throw new IllegalStateException("O pacote precisa estar pronto antes desta ação.");
        }
        if ("VALUE_MOMENT".equals(eventType)) {
            requireConfirmation(request, "O reconhecimento de valor exige confirmação e justificativa.");
            return;
        }
        if ("READY_RESULT_USED".equals(eventType)) {
            if (!session.events.contains("VALUE_MOMENT")
                    || LOOKS.stream().noneMatch(look -> look.id().equals(trim(request.selectedLookId())))) {
                throw new IllegalStateException("O uso exige valor reconhecido e uma combinação existente.");
            }
            return;
        }
        if ("SAVE_INTEREST_DECLARED".equals(eventType)) {
            requireConfirmation(request, "O interesse de retorno exige confirmação e justificativa.");
            if (!session.events.contains("READY_RESULT_USED")) {
                throw new IllegalStateException("O interesse de retorno exige um resultado realmente usado.");
            }
            return;
        }
        if ("RETURN_COMPLETED".equals(eventType)) {
            if (!session.events.contains("AUTHENTICATED_ACCESS_RESTORED")
                    || session.resultPackageId == null) {
                throw new IllegalStateException("O retorno exige acesso autenticado ao mesmo pacote.");
            }
            return;
        }
        if ("PREFERRED_OVER_FREE".equals(eventType)) {
            requireConfirmation(request, "A preferência exige escolha explícita e justificativa.");
            if (!session.events.contains("RETURN_COMPLETED")
                    || !"FREE_SEARCH".equals(trim(request.alternativeCode()))) {
                throw new IllegalStateException(
                        "A preferência exige retorno comprovado e comparação com a alternativa gratuita.");
            }
            return;
        }
        if ("CHECKOUT_STARTED".equals(eventType)) {
            if (!session.events.contains("PREFERRED_OVER_FREE")) {
                throw new IllegalStateException("O checkout simulado exige preferência registrada.");
            }
            return;
        }
        if ("RECOVERY_COMPLETED".equals(eventType)
                && (!"RECOVERY".equals(session.scenarioCode)
                        || !session.events.contains("RETURN_COMPLETED"))) {
            throw new IllegalStateException("A recuperação exige o mesmo resultado retomado e utilizado.");
        }
    }

    /** Exige confirmação sintética explícita e uma justificativa auditável. */
    private void requireConfirmation(EventRequest request, String message) {
        if (!Boolean.TRUE.equals(request.confirmed()) || trim(request.justification()).length() < 5) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Confirma os cinco marcos canônicos sem contar mera apresentação como uso. */
    private boolean canonicalFunnelComplete(StoredSession session) {
        return session.events.containsAll(Set.of(
                "EXPERIENCE_STARTED",
                "VALUE_MOMENT",
                "READY_RESULT_USED",
                "PREFERRED_OVER_FREE",
                "CHECKOUT_STARTED"));
    }

    /** Confirma a versão e a aceitação explícita da política interna de continuidade. */
    private void validatePolicy(Boolean acknowledged, String policyVersion) {
        if (!Boolean.TRUE.equals(acknowledged)
                || !CONTINUITY_POLICY_VERSION.equals(trim(policyVersion))) {
            throw new IllegalArgumentException(
                    "A política interna de continuidade precisa ser aceita na versão ativa.");
        }
    }

    /** Bloqueia dados e pedidos excluídos do protótipo estático sem tentar interpretá-los. */
    private boolean unsafe(InputSnapshot input) {
        String text = normalized(String.join(" ",
                input.occasion(),
                String.join(" ", input.preferences()),
                String.join(" ", input.constraints()),
                String.join(" ", input.pieceReferences())));
        return List.of(
                        "foto corporal",
                        "meu corpo",
                        "tipo de corpo",
                        "comprar roupa",
                        "comprar uma roupa",
                        "atraente",
                        "emagrecer",
                        "diagnostico")
                .stream()
                .anyMatch(text::contains);
    }

    /** Normaliza texto somente para aplicar limites determinísticos de segurança. */
    private String normalized(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT);
    }

    /** Registra uma vez o evento e conserva a segregação das métricas comerciais. */
    private void recordOnce(StoredSession session, String eventType) {
        if (session.events.contains(eventType)) return;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("productId", PRODUCT_ID);
        metadata.put("experienceVersion", session.prototypeVersion);
        metadata.put("trafficClass", "AGENT_VALIDATION");
        metadata.put("mh_internal_test", true);
        metadata.put("actorType", "TEST_AGENT");
        metadata.put("validationEligible", false);
        metadata.put("commercialEvidenceEligible", false);
        metadata.put("sourceReference", SOURCE_REFERENCE);
        metadata.put("scenarioCode", session.scenarioCode);
        metadata.put("readingId", session.readingId);
        metadata.put("executionId", session.executionId);
        metadata.put("evidenceRef", session.evidenceId + ":" + eventType);
        metadata.put("fixtureContract", FIXTURE_CONTRACT);
        metadata.put("providerCalls", 0);
        metadata.put("providerCostUsd", 0);
        metadata.put("checkoutMode", "SIMULATED_NO_CHARGE");
        metadata.put("paymentEnabled", false);
        metadata.put("published", false);
        metadata.put("mediaSpendBrl", 0);
        eventAudit(session).add(new RecordedEvent(eventType, now().toString(), Map.copyOf(metadata)));
        session.events.add(eventType);
        log.info(
                "Evento sintético Alcyone registrado; evidenceId={} eventType={} scenarioCode={} trafficClass=AGENT_VALIDATION",
                session.evidenceId,
                eventType,
                session.scenarioCode);
    }

    /** Recupera a auditoria e adapta checkpoints anteriores que não possuíam a lista detalhada. */
    private List<RecordedEvent> eventAudit(StoredSession session) {
        if (session.eventAudit == null) session.eventAudit = new ArrayList<>();
        return session.eventAudit;
    }

    /** Exige uma sessão v2 ativa por hash e rejeita credenciais vencidas. */
    private StoredSession requiredSession(String sessionToken) {
        StoredSession session = sessions.get(hash(sessionToken));
        if (session == null
                || !VERSION.equals(session.prototypeVersion)
                || session.sessionExpiresAt == null
                || !now().isBefore(Instant.parse(session.sessionExpiresAt))) {
            throw new SecurityException("Sessão privada inválida ou expirada.");
        }
        return session;
    }

    /** Restringe o motor à identidade declarada pelo contrato versionado atual. */
    private void requireProduct(String productSlug) {
        if (!PRODUCT_SLUG.equals(trim(productSlug))) {
            throw new IllegalArgumentException("Produto visual não suportado por este contrato.");
        }
    }

    /** Cria uma projeção sem devolver credenciais persistidas ou dados de outros cenários. */
    private SessionResponse response(StoredSession session, String issuedSessionToken) {
        return new SessionResponse(
                issuedSessionToken,
                PRODUCT_ID,
                PRODUCT_SLUG,
                session.prototypeVersion,
                SOURCE_REFERENCE,
                session.scenarioCode,
                "AGENT_VALIDATION",
                true,
                session.status,
                session.input,
                List.copyOf(session.looks),
                session.selectedLookId,
                session.blocker,
                List.copyOf(session.events),
                "SIMULATED_NO_CHARGE",
                session.finished,
                session.evidenceId,
                session.resultPackageId,
                session.resultPackageFingerprint,
                session.sessionExpiresAt,
                session.continuityPolicyVersion,
                session.policyAcknowledgedAt != null,
                0,
                0);
    }

    /** Emite uma credencial opaca de alta entropia sem significado de negócio. */
    private String credential() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    /** Deriva a identidade persistível de uma credencial sem conservar seu valor bruto. */
    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(trim(value).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível para proteger credenciais.", ex);
        }
    }

    /** Obtém o instante controlado usado em validade e auditoria. */
    private Instant now() {
        return clock.instant();
    }

    /** Remove espaços periféricos e valores nulos antes da validação de identidade. */
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    /** Normaliza os itens de entrada preservando a ordem escolhida pelo cenário. */
    private List<String> trimmed(List<String> values) {
        return values.stream().map(String::trim).toList();
    }

    /** Recupera checkpoints e invalida credenciais brutas legadas antes de regravar o arquivo. */
    private void load() {
        if (!Files.exists(storagePath)) return;
        try {
            Map<String, StoredSession> loaded =
                    json.readValue(storagePath.toFile(), new TypeReference<>() {});
            boolean migrated = false;
            for (Map.Entry<String, StoredSession> entry : loaded.entrySet()) {
                StoredSession session = entry.getValue();
                if (!VERSION.equals(session.prototypeVersion) || trim(session.sessionTokenHash).isEmpty()) {
                    session.prototypeVersion = session.prototypeVersion == null
                            ? PREVIOUS_VERSION
                            : session.prototypeVersion;
                    session.sessionTokenHash = "legacy-" + hash(entry.getKey());
                    session.sessionExpiresAt = Instant.EPOCH.toString();
                    session.continuationCredentialHash = null;
                    session.continuityExpiresAt = null;
                    session.sessionToken = null;
                    migrated = true;
                }
                sessions.put(session.sessionTokenHash, session);
            }
            if (migrated) persist();
        } catch (Exception ex) {
            log.error("Falha ao carregar sessões visuais multiagente; storagePath={}", storagePath, ex);
            throw new IllegalStateException("Não foi possível recuperar as sessões do protótipo.", ex);
        }
    }

    /** Grava checkpoints atomicamente para garantir retomada após reinício. */
    private void persist() {
        try {
            Files.createDirectories(storagePath.getParent());
            Path temporary = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
            json.writeValue(temporary.toFile(), sessions);
            Files.move(
                    temporary,
                    storagePath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            log.error("Falha ao persistir sessões visuais multiagente; storagePath={}", storagePath, ex);
            throw new IllegalStateException("Não foi possível preservar a sessão do protótipo.", ex);
        }
    }

    /** Solicita uma sessão fresca para um cenário sintético canônico. */
    public record AgentSessionRequest(
            @NotBlank @Size(max = 200) String sourceReference,
            @NotBlank @Size(max = 32) String scenarioCode) {}

    /** Entrada limitada à decisão de look para uma ocasião concreta. */
    public record InputRequest(
            @NotBlank @Size(max = 160) String occasion,
            @jakarta.validation.constraints.NotNull LocalDate eventDate,
            @NotEmpty @Size(max = 5) List<@NotBlank @Size(max = 120) String> preferences,
            @NotEmpty @Size(max = 5) List<@NotBlank @Size(max = 160) String> constraints,
            @NotEmpty @Size(min = 2, max = 8) List<@NotBlank @Size(max = 160) String> pieceReferences) {}

    /** Entrada persistida e imutável depois do primeiro sinal explícito de valor. */
    public record InputSnapshot(
            String occasion,
            LocalDate eventDate,
            List<String> preferences,
            List<String> constraints,
            List<String> pieceReferences) {}

    /** Ação observável permitida pelo contrato sintético, sem dado pessoal. */
    public record EventRequest(
            @NotBlank @Size(max = 64) String eventType,
            Boolean confirmed,
            @Size(max = 64) String selectedLookId,
            @Size(max = 64) String alternativeCode,
            @Size(max = 500) String justification) {}

    /** Aceite versionado exigido antes da criação da credencial de continuidade. */
    public record ContinuityCredentialRequest(
            Boolean policyAcknowledged,
            @NotBlank @Size(max = 80) String policyVersion) {}

    /** Credencial apresentada para obter nova sessão e nova credencial rotacionada. */
    public record ContinuityResumeRequest(
            @NotBlank @Size(max = 160) String continuationCredential,
            @NotBlank @Size(max = 80) String policyVersion) {}

    /** Token de sessão recebido somente pela operação interna de expiração do harness. */
    public record InternalSessionExpirationRequest(
            @NotBlank @Size(max = 160) String sessionToken) {}

    /** Combinação visual determinística acompanhada de orientação prática. */
    public record LookCard(
            String id, String title, String imagePath, String whyItWorks, String practicalAdjustment) {}

    /** Contrato sanitizado da superfície privada de homologação. */
    public record ContractResponse(
            long productId,
            String productSlug,
            String prototypeVersion,
            String experienceName,
            String fixtureContract,
            String fixtureManifestPath,
            String productStatus,
            String checkoutMode,
            String continuityPolicyVersion,
            long sessionTtlSeconds,
            long continuityTtlSeconds,
            List<String> instrumentationEvents,
            boolean published,
            boolean paymentEnabled,
            int mediaSpendBrl,
            int providerCallsAuthorized) {}

    /** Estado retomável de uma única sessão segregada. */
    public record SessionResponse(
            String sessionToken,
            long productId,
            String productSlug,
            String prototypeVersion,
            String sourceReference,
            String scenarioCode,
            String trafficClass,
            boolean mhInternalTest,
            String status,
            InputSnapshot input,
            List<LookCard> looks,
            String selectedLookId,
            String blocker,
            List<String> events,
            String checkoutMode,
            boolean finished,
            String evidenceId,
            String resultPackageId,
            String resultPackageFingerprint,
            String sessionExpiresAt,
            String continuityPolicyVersion,
            boolean policyAcknowledged,
            int providerCalls,
            int providerCostUsd) {}

    /** Segredo emitido uma vez para viabilizar retorno autenticado futuro. */
    public record ContinuityCredentialResponse(
            String policyVersion,
            String policyAcknowledgedAt,
            String continuityExpiresAt,
            String resultPackageId,
            String continuationCredential) {}

    /** Resultado da retomada com ambas as credenciais rotacionadas. */
    public record ContinuityResumeResponse(
            String sessionToken,
            String sessionExpiresAt,
            String continuationCredential,
            String continuityExpiresAt,
            String resultPackageId,
            SessionResponse session) {}

    /** Pacote funcional devolvido somente ao proprietário autenticado. */
    public record ResultPackageResponse(
            String resultPackageId, String resultPackageFingerprint, List<LookCard> looks) {}

    /** Evidência funcional sem credencial, contato ou conversão comercial. */
    public record AgentValidationEvidence(
            long productId,
            String productSlug,
            String prototypeVersion,
            String sourceReference,
            String scenarioCode,
            String trafficClass,
            boolean mhInternalTest,
            String evidenceId,
            String readingId,
            String executionId,
            String status,
            InputSnapshot input,
            List<LookCard> looks,
            String selectedLookId,
            String blocker,
            List<String> events,
            boolean finished,
            String resultPackageId,
            String resultPackageFingerprint,
            String fixtureContract,
            String continuityPolicyVersion,
            String policyAcknowledgedAt,
            String sessionExpiresAt,
            String continuityExpiresAt,
            boolean credentialStoredAsHash,
            List<RecordedEvent> eventAudit,
            int providerCalls,
            int providerCostUsd,
            Map<String, Object> sideEffects,
            boolean humanEvidenceClaimed,
            boolean commercialEvidenceClaimed) {}

    /** Evento sintético persistido com horário, segregação e correlação da execução. */
    public record RecordedEvent(String eventType, String occurredAt, Map<String, Object> metadata) {}

    /** Checkpoint interno de uma execução isolada do harness. */
    public static final class StoredSession {
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public String sessionToken;
        public String sessionTokenHash;
        public String sessionExpiresAt;
        public String continuationCredentialHash;
        public String continuityExpiresAt;
        public String continuityPolicyVersion;
        public String policyAcknowledgedAt;
        public String prototypeVersion;
        public String evidenceId;
        public String sourceReference;
        public String scenarioCode;
        public String readingId;
        public String executionId;
        public String status = "AUTHORIZED";
        public InputSnapshot input;
        public List<LookCard> looks = List.of();
        public String selectedLookId;
        public String blocker;
        public Set<String> events = new LinkedHashSet<>();
        public List<RecordedEvent> eventAudit = new ArrayList<>();
        public boolean finished;
        public String resultPackageId;
        public String resultPackageFingerprint;

        /** Construtor vazio utilizado exclusivamente pela persistência JSON. */
        public StoredSession() {}
    }
}
