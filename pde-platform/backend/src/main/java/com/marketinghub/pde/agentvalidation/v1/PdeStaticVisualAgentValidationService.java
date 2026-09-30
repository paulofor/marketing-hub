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
    private static final String VERSION = "alcyone-private-v3";
    private static final String PREVIOUS_VERSION = "alcyone-private-v2";
    private static final String FIXTURE_CONTRACT = "PDE_STATIC_RESULT_FIXTURES_V1";
    private static final String INTAKE_CONSENT_VERSION = "ALCYONE_AGENT_INTAKE_CONSENT_V1";
    private static final String CONTINUITY_POLICY_VERSION = "ALCYONE_AGENT_CONTINUITY_V1";
    private static final String SAFETY_OUTCOME_CODE = "OUT_OF_SCOPE";
    private static final String SAFETY_NO_RESULT_MESSAGE =
            "Nenhuma combinação foi criada e nenhuma chamada externa aconteceu.";
    private static final String SAFETY_SAFE_ACTION =
            "Inicie uma nova execução usando somente referências isoladas das peças, "
                    + "sem foto corporal, recomendação de compra ou julgamento do corpo.";
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final Duration CONTINUITY_TTL = Duration.ofHours(24);
    private static final Set<String> SCENARIOS = Set.of("ADHERENT", "RECOVERY", "SAFETY");
    private static final List<String> CANONICAL_EVENTS = List.of(
            "EXPERIENCE_STARTED",
            "VALUE_MOMENT",
            "READY_RESULT_USED",
            "PREFERRED_OVER_FREE",
            "CHECKOUT_STARTED");
    private static final Set<String> EXPLICIT_EVENTS = Set.of(
            "VALUE_MOMENT", "READY_RESULT_USED", "PREFERRED_OVER_FREE", "CHECKOUT_STARTED");
    private static final List<ErrorStateContract> ERROR_STATES = List.of(
            new ErrorStateContract(
                    "ACCESS_INVALID",
                    "Este acesso privado não é válido para esta experiência.",
                    "Solicitar ao harness nova concessão para o mesmo cenário e versão."),
            new ErrorStateContract(
                    "SESSION_EXPIRED",
                    "Sua sessão terminou, mas o trabalho preservado pode ser retomado.",
                    "Autenticar a credencial de continuidade e recuperar a mesma execução."),
            new ErrorStateContract(
                    "INPUT_INCOMPLETE",
                    "Ainda faltam informações necessárias para preparar a seleção.",
                    "Destacar os campos faltantes sem chamar o executor."),
            new ErrorStateContract(
                    "HARNESS_FAILURE",
                    "Não foi possível concluir o processamento agora; sua entrada foi preservada.",
                    "Repetir com a mesma execução e sem duplicar pacote ou custo."),
            new ErrorStateContract(
                    "RESULT_UNAVAILABLE",
                    "Sua seleção está preservada, mas não pode ser exibida neste momento.",
                    "Recarregar o mesmo pacote autorizado sem regeneração."),
            new ErrorStateContract(
                    "RESUME_FAILED",
                    "Não foi possível retomar com esta credencial.",
                    "Validar versão e vigência sem criar nova leitura."));
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
                INTAKE_CONSENT_VERSION,
                CONTINUITY_POLICY_VERSION,
                SESSION_TTL.toSeconds(),
                CONTINUITY_TTL.toSeconds(),
                CANONICAL_EVENTS,
                ERROR_STATES,
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

    /** Persiste o consentimento sintético versionado antes de aceitar qualquer dado de entrada. */
    public synchronized SessionResponse acceptConsent(
            String productSlug, String sessionToken, ConsentRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        if (!Boolean.TRUE.equals(request.accepted())
                || !INTAKE_CONSENT_VERSION.equals(trim(request.consentVersion()))) {
            throw new IllegalArgumentException(
                    "O consentimento sintético precisa ser aceito na versão ativa antes da entrada.");
        }
        if (session.input != null) {
            throw new IllegalStateException("O consentimento não pode mudar depois da entrada.");
        }
        if (session.consentedAt == null) {
            session.consentVersion = INTAKE_CONSENT_VERSION;
            session.consentedAt = now().toString();
            persist();
        }
        return response(session, null);
    }

    /** Persiste a entrada mínima e registra início somente depois de aceitá-la. */
    public synchronized SessionResponse saveInput(
            String productSlug, String sessionToken, InputRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        if (!INTAKE_CONSENT_VERSION.equals(session.consentVersion) || session.consentedAt == null) {
            throw new SecurityException("O consentimento versionado é obrigatório antes da entrada.");
        }
        validateInput(request);
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
        if (session.inputAcceptedAt == null) session.inputAcceptedAt = now().toString();
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
            if (session.safetyBlockedAt == null) session.safetyBlockedAt = now().toString();
            persist();
            return response(session, null);
        }
        session.looks = LOOKS;
        session.status = "READY";
        session.blocker = null;
        session.resultPackageId = UUID.randomUUID().toString();
        session.resultPackageFingerprint =
                "alcyone-static-fixtures-v1:3x1024x1024:provider-calls-0";
        if (session.resultReadyAt == null) session.resultReadyAt = now().toString();
        persist();
        return response(session, null);
    }

    /** Confirma que o navegador apresentou o pacote exato sem converter apresentação em valor. */
    public synchronized SessionResponse markResultPresented(
            String productSlug, String sessionToken, PackageMilestoneRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        requirePackage(session, request.resultPackageId(), request.resultPackageFingerprint());
        if (session.resultPresentedAt == null) session.resultPresentedAt = now().toString();
        persist();
        return response(session, null);
    }

    /** Persiste o interesse explícito de retorno como estado, sem criar um sinal de funil. */
    public synchronized SessionResponse declareSaveInterest(
            String productSlug, String sessionToken, ConfirmationRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        requireConfirmation(
                request.confirmed(),
                request.justification(),
                "O interesse de retorno exige confirmação e justificativa.");
        if (!session.events.contains("READY_RESULT_USED")) {
            throw new IllegalStateException("O interesse de retorno exige um resultado realmente usado.");
        }
        if (session.saveInterestAt == null) session.saveInterestAt = now().toString();
        persist();
        return response(session, null);
    }

    /** Registra apenas sinais explícitos na sequência funcional do cenário sintético. */
    public synchronized SessionResponse event(
            String productSlug, String sessionToken, EventRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        String eventType = request.eventType().trim().toUpperCase();
        if (!EXPLICIT_EVENTS.contains(eventType)) {
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
                || session.saveInterestAt == null) {
            throw new IllegalStateException(
                    "A continuidade exige resultado usado e interesse de retorno declarado.");
        }
        String continuationCredential = credential();
        session.continuityPolicyVersion = CONTINUITY_POLICY_VERSION;
        session.policyAcknowledgedAt = now().toString();
        session.continuationCredentialHash = hash(continuationCredential);
        session.continuityExpiresAt = now().plus(CONTINUITY_TTL).toString();
        if (session.continuityCredentialCreatedAt == null) {
            session.continuityCredentialCreatedAt = now().toString();
        }
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
        session.accessAuthenticatedAt = now().toString();
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

    /** Registra a autorização concluída do pacote depois da autenticação, sem inferir retorno. */
    public synchronized SessionResponse markAccessCompleted(
            String productSlug, String sessionToken, PackageMilestoneRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        if (session.accessAuthenticatedAt == null) {
            throw new IllegalStateException("O acesso ao pacote exige autenticação de continuidade.");
        }
        requirePackage(session, request.resultPackageId(), request.resultPackageFingerprint());
        if (session.accessCompletedAt == null) session.accessCompletedAt = now().toString();
        persist();
        return response(session, null);
    }

    /** Confirma o retorno somente após o cliente validar e autorizar o pacote preservado. */
    public synchronized SessionResponse markReturnCompleted(
            String productSlug, String sessionToken, PackageMilestoneRequest request) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        requirePackage(session, request.resultPackageId(), request.resultPackageFingerprint());
        if (session.accessCompletedAt == null) {
            throw new IllegalStateException("O retorno exige acesso concluído ao mesmo pacote.");
        }
        if (session.returnedAt == null) session.returnedAt = now().toString();
        persist();
        return response(session, null);
    }

    /** Encerra o cenário somente quando seus sinais e estados obrigatórios estão completos. */
    public synchronized SessionResponse completeScenario(String productSlug, String sessionToken) {
        requireProduct(productSlug);
        StoredSession session = requiredSession(sessionToken);
        boolean safety = "SAFETY".equals(session.scenarioCode)
                && "BLOCKED".equals(session.status)
                && session.safetyBlockedAt != null;
        boolean journey = !"SAFETY".equals(session.scenarioCode)
                && canonicalFunnelComplete(session)
                && continuityComplete(session);
        if (!safety && !journey) {
            throw new IllegalStateException("O cenário não possui toda a evidência funcional exigida.");
        }
        if ("RECOVERY".equals(session.scenarioCode) && session.recoveryCompletedAt == null) {
            session.recoveryCompletedAt = now().toString();
        }
        session.finished = true;
        if (session.finishedAt == null) session.finishedAt = now().toString();
        persist();
        return response(session, null);
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
                safetyOutcome(session),
                canonicalEvents(session),
                session.finished,
                session.resultPackageId,
                session.resultPackageFingerprint,
                FIXTURE_CONTRACT,
                session.consentVersion,
                session.consentedAt,
                session.continuityPolicyVersion,
                session.policyAcknowledgedAt,
                session.sessionExpiresAt,
                session.continuityExpiresAt,
                session.continuationCredentialHash != null,
                milestones(session),
                canonicalEventAudit(session),
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
        if (!"READY".equals(session.status)) {
            throw new IllegalStateException("O pacote precisa estar pronto antes desta ação.");
        }
        if ("VALUE_MOMENT".equals(eventType)) {
            if (session.resultPresentedAt == null) {
                throw new IllegalStateException("O valor só pode ser confirmado após a apresentação do pacote.");
            }
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
        if ("PREFERRED_OVER_FREE".equals(eventType)) {
            requireConfirmation(request, "A preferência exige escolha explícita e justificativa.");
            if (session.returnedAt == null || !"FREE_SEARCH".equals(trim(request.alternativeCode()))) {
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
    }

    /** Exige confirmação sintética explícita e uma justificativa auditável. */
    private void requireConfirmation(EventRequest request, String message) {
        requireConfirmation(request.confirmed(), request.justification(), message);
    }

    /** Exige confirmação e justificativa sem acoplar a validação a um tipo de request. */
    private void requireConfirmation(Boolean confirmed, String justification, String message) {
        if (!Boolean.TRUE.equals(confirmed) || trim(justification).length() < 5) {
            throw new IllegalArgumentException(message);
        }
    }

    /** Confirma os cinco marcos canônicos sem contar mera apresentação como uso. */
    private boolean canonicalFunnelComplete(StoredSession session) {
        return session.events.containsAll(CANONICAL_EVENTS);
    }

    /** Confirma os marcos distintos de interesse, credencial, autenticação, acesso e retorno. */
    private boolean continuityComplete(StoredSession session) {
        return session.saveInterestAt != null
                && session.continuityCredentialCreatedAt != null
                && session.accessAuthenticatedAt != null
                && session.accessCompletedAt != null
                && session.returnedAt != null;
    }

    /** Confirma a versão e a aceitação explícita da política interna de continuidade. */
    private void validatePolicy(Boolean acknowledged, String policyVersion) {
        if (!Boolean.TRUE.equals(acknowledged)
                || !CONTINUITY_POLICY_VERSION.equals(trim(policyVersion))) {
            throw new IllegalArgumentException(
                    "A política interna de continuidade precisa ser aceita na versão ativa.");
        }
    }

    /** Rejeita entrada incompleta mesmo quando o serviço é chamado fora do controller validado. */
    private void validateInput(InputRequest request) {
        if (request == null
                || trim(request.occasion()).length() < 3
                || request.eventDate() == null
                || request.preferences() == null
                || request.preferences().isEmpty()
                || request.constraints() == null
                || request.constraints().isEmpty()
                || request.pieceReferences() == null
                || request.pieceReferences().size() < 2
                || request.preferences().stream().anyMatch(value -> trim(value).isEmpty())
                || request.constraints().stream().anyMatch(value -> trim(value).isEmpty())
                || request.pieceReferences().stream().anyMatch(value -> trim(value).isEmpty())) {
            throw new IllegalArgumentException(
                    "Ainda faltam informações necessárias para preparar a seleção.");
        }
    }

    /** Garante que um marco se refere ao pacote e fingerprint da própria sessão. */
    private void requirePackage(StoredSession session, String packageId, String fingerprint) {
        if (!"READY".equals(session.status)
                || session.resultPackageId == null
                || !session.resultPackageId.equals(trim(packageId))
                || !session.resultPackageFingerprint.equals(trim(fingerprint))) {
            throw new SecurityException("O pacote não pertence à sessão autenticada.");
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

    /** Projeta somente os cinco sinais canônicos na ordem em que foram observados. */
    private List<String> canonicalEvents(StoredSession session) {
        if (session.events == null) session.events = new LinkedHashSet<>();
        return CANONICAL_EVENTS.stream().filter(session.events::contains).toList();
    }

    /** Remove marcos operacionais legados da telemetria exposta ao funil. */
    private List<RecordedEvent> canonicalEventAudit(StoredSession session) {
        return eventAudit(session).stream()
                .filter(event -> CANONICAL_EVENTS.contains(event.eventType()))
                .toList();
    }

    /** Reúne os marcos anuláveis sem inferir um estado a partir do marco anterior. */
    private ValidationMilestones milestones(StoredSession session) {
        return new ValidationMilestones(
                session.inputAcceptedAt,
                session.resultReadyAt,
                session.resultPresentedAt,
                session.saveInterestAt,
                session.continuityCredentialCreatedAt,
                session.accessAuthenticatedAt,
                session.accessCompletedAt,
                session.returnedAt,
                session.safetyBlockedAt,
                session.recoveryCompletedAt,
                session.finishedAt);
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
                safetyOutcome(session),
                canonicalEvents(session),
                "SIMULATED_NO_CHARGE",
                session.finished,
                session.evidenceId,
                session.resultPackageId,
                session.resultPackageFingerprint,
                session.sessionExpiresAt,
                session.consentVersion,
                session.consentedAt,
                session.continuityPolicyVersion,
                session.policyAcknowledgedAt != null,
                milestones(session),
                0,
                0);
    }

    /** Projeta causa, ausência de resultado e ação segura somente para um bloqueio comprovado. */
    private SafetyOutcome safetyOutcome(StoredSession session) {
        if (!"SAFETY".equals(session.scenarioCode)
                || !"BLOCKED".equals(session.status)
                || session.safetyBlockedAt == null
                || trim(session.blocker).isEmpty()) {
            return null;
        }
        return new SafetyOutcome(
                SAFETY_OUTCOME_CODE,
                session.blocker,
                SAFETY_NO_RESULT_MESSAGE,
                SAFETY_SAFE_ACTION,
                false,
                false);
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
                if (VERSION.equals(session.prototypeVersion)) {
                    migrated |= migrateOperationalEvents(session);
                    if (!INTAKE_CONSENT_VERSION.equals(session.consentVersion)
                            || session.consentedAt == null) {
                        session.sessionExpiresAt = Instant.EPOCH.toString();
                        session.continuationCredentialHash = null;
                        session.continuityExpiresAt = null;
                        migrated = true;
                    }
                }
                sessions.put(session.sessionTokenHash, session);
            }
            if (migrated) persist();
        } catch (Exception ex) {
            log.error("Falha ao carregar sessões visuais multiagente; storagePath={}", storagePath, ex);
            throw new IllegalStateException("Não foi possível recuperar as sessões do protótipo.", ex);
        }
    }

    /** Converte eventos operacionais da primeira revisão v2 em timestamps históricos não comerciais. */
    private boolean migrateOperationalEvents(StoredSession session) {
        if (session.events == null) session.events = new LinkedHashSet<>();
        Map<String, String> timestamps = new LinkedHashMap<>();
        for (RecordedEvent event : eventAudit(session)) {
            timestamps.putIfAbsent(event.eventType(), event.occurredAt());
        }
        if (session.saveInterestAt == null) {
            session.saveInterestAt = timestamps.get("SAVE_INTEREST_DECLARED");
        }
        if (session.continuityCredentialCreatedAt == null) {
            session.continuityCredentialCreatedAt = timestamps.get("CONTINUITY_CREDENTIAL_CREATED");
        }
        if (session.accessAuthenticatedAt == null) {
            session.accessAuthenticatedAt = timestamps.get("AUTHENTICATED_ACCESS_RESTORED");
        }
        if (session.accessCompletedAt == null) {
            session.accessCompletedAt = timestamps.get("RETURN_COMPLETED");
        }
        if (session.returnedAt == null) session.returnedAt = timestamps.get("RETURN_COMPLETED");
        if (session.safetyBlockedAt == null) {
            session.safetyBlockedAt = timestamps.get("SAFETY_LIMIT_BLOCKED");
        }
        if (session.recoveryCompletedAt == null) {
            session.recoveryCompletedAt = timestamps.get("RECOVERY_COMPLETED");
        }
        if (session.finishedAt == null) {
            session.finishedAt = timestamps.get("AGENT_SCENARIO_COMPLETED");
        }
        boolean hadOperationalEvents = session.events.removeIf(event -> !CANONICAL_EVENTS.contains(event));
        boolean hadOperationalAudit = eventAudit(session).removeIf(
                event -> !CANONICAL_EVENTS.contains(event.eventType()));
        return hadOperationalEvents || hadOperationalAudit;
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

    /** Consentimento sintético versionado exigido antes da entrada do agente. */
    public record ConsentRequest(
            Boolean accepted,
            @NotBlank @Size(max = 80) String consentVersion) {}

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

    /** Confirmação auditável de um marco operacional que não pertence ao funil. */
    public record ConfirmationRequest(
            Boolean confirmed,
            @Size(max = 500) String justification) {}

    /** Identidade exata do pacote usada para confirmar apresentação, acesso ou retorno. */
    public record PackageMilestoneRequest(
            @NotBlank @Size(max = 80) String resultPackageId,
            @NotBlank @Size(max = 160) String resultPackageFingerprint) {}

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
            String intakeConsentVersion,
            String continuityPolicyVersion,
            long sessionTtlSeconds,
            long continuityTtlSeconds,
            List<String> instrumentationEvents,
            List<ErrorStateContract> errorStates,
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
            SafetyOutcome safetyOutcome,
            List<String> events,
            String checkoutMode,
            boolean finished,
            String evidenceId,
            String resultPackageId,
            String resultPackageFingerprint,
            String sessionExpiresAt,
            String consentVersion,
            String consentedAt,
            String continuityPolicyVersion,
            boolean policyAcknowledged,
            ValidationMilestones milestones,
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
            SafetyOutcome safetyOutcome,
            List<String> events,
            boolean finished,
            String resultPackageId,
            String resultPackageFingerprint,
            String fixtureContract,
            String consentVersion,
            String consentedAt,
            String continuityPolicyVersion,
            String policyAcknowledgedAt,
            String sessionExpiresAt,
            String continuityExpiresAt,
            boolean credentialStoredAsHash,
            ValidationMilestones milestones,
            List<RecordedEvent> eventAudit,
            int providerCalls,
            int providerCostUsd,
            Map<String, Object> sideEffects,
            boolean humanEvidenceClaimed,
            boolean commercialEvidenceClaimed) {}

    /** Desfecho seguro e explicável de um pedido recusado antes de produzir resultado. */
    public record SafetyOutcome(
            String code,
            String reason,
            String noResultMessage,
            String safeAction,
            boolean resultGenerated,
            boolean providerCalled) {}

    /** Mensagem e recuperação determinística de um estado de falha previsto. */
    public record ErrorStateContract(String code, String message, String recoveryAction) {}

    /** Marcos operacionais independentes que permanecem nulos até a ação correspondente. */
    public record ValidationMilestones(
            String inputAcceptedAt,
            String resultReadyAt,
            String resultPresentedAt,
            String saveInterestAt,
            String continuityCredentialCreatedAt,
            String accessAuthenticatedAt,
            String accessCompletedAt,
            String returnedAt,
            String safetyBlockedAt,
            String recoveryCompletedAt,
            String finishedAt) {}

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
        public String consentVersion;
        public String consentedAt;
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
        public String inputAcceptedAt;
        public String resultReadyAt;
        public String resultPresentedAt;
        public String saveInterestAt;
        public String continuityCredentialCreatedAt;
        public String accessAuthenticatedAt;
        public String accessCompletedAt;
        public String returnedAt;
        public String safetyBlockedAt;
        public String recoveryCompletedAt;
        public String finishedAt;
        public boolean finished;
        public String resultPackageId;
        public String resultPackageFingerprint;

        /** Construtor vazio utilizado exclusivamente pela persistência JSON. */
        public StoredSession() {}
    }
}
