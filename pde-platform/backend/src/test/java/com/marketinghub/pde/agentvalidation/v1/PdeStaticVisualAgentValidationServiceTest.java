package com.marketinghub.pde.agentvalidation.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar a continuidade visual sintética sem evidência humana ou efeito externo. */
class PdeStaticVisualAgentValidationServiceTest {
    private static final String PRODUCT = "pde-planejado-46";
    private static final String POLICY = "ALCYONE_AGENT_CONTINUITY_V1";
    @TempDir Path temporaryDirectory;
    private final MutableClock clock = new MutableClock(Instant.parse("2026-09-30T10:00:00Z"));

    /** Conclui sinais, continuidade e checkout com três imagens estáticas e custo externo zero. */
    @Test
    void completesAdherentScenarioWithAuthenticatedReturn() {
        var service = service();
        var start = start(service, "ADHERENT");
        String initialToken = start.sessionToken();

        var inputReady = service.saveInput(PRODUCT, initialToken, validInput());
        var generated = generateAndPresent(service, initialToken);
        event(service, initialToken, "VALUE_MOMENT", true, null, null, "As opções resolvem a decisão.");
        event(service, initialToken, "READY_RESULT_USED", null, "look-1", null, null);
        var access = createAndResume(service, initialToken);
        event(
                service,
                access.sessionToken(),
                "PREFERRED_OVER_FREE",
                true,
                null,
                "FREE_SEARCH",
                "É mais direto que pesquisar referências soltas.");
        event(service, access.sessionToken(), "CHECKOUT_STARTED", null, null, null, null);
        var finished = service.completeScenario(PRODUCT, access.sessionToken());
        var evidence = service.evidence(PRODUCT, start.evidenceId());

        assertThat(inputReady.events()).containsExactly("EXPERIENCE_STARTED");
        assertThat(generated.looks()).hasSize(3).allMatch(look -> look.imagePath().endsWith(".png"));
        assertThat(finished.finished()).isTrue();
        assertThat(evidence.events()).containsExactly(
                "EXPERIENCE_STARTED",
                "VALUE_MOMENT",
                "READY_RESULT_USED",
                "PREFERRED_OVER_FREE",
                "CHECKOUT_STARTED");
        assertThat(evidence.consentedAt()).isNotBlank();
        assertThat(evidence.milestones().inputAcceptedAt()).isNotBlank();
        assertThat(evidence.milestones().resultPresentedAt()).isNotBlank();
        assertThat(evidence.milestones().saveInterestAt()).isNotBlank();
        assertThat(evidence.milestones().continuityCredentialCreatedAt()).isNotBlank();
        assertThat(evidence.milestones().accessAuthenticatedAt()).isNotBlank();
        assertThat(evidence.milestones().accessCompletedAt()).isNotBlank();
        assertThat(evidence.milestones().returnedAt()).isNotBlank();
        assertThat(evidence.prototypeVersion()).isEqualTo("alcyone-private-v2");
        assertThat(evidence.credentialStoredAsHash()).isTrue();
        assertThat(evidence.providerCalls()).isZero();
        assertThat(evidence.sideEffects())
                .containsEntry("paymentEnabled", false)
                .containsEntry("published", false)
                .containsEntry("campaignCreated", false)
                .containsEntry("mediaSpendBrl", 0);
        assertThat(evidence.humanEvidenceClaimed()).isFalse();
        assertThat(evidence.commercialEvidenceClaimed()).isFalse();
    }

    /** Retoma o mesmo pacote após reinício e expiração real da sessão curta. */
    @Test
    void resumesSamePackageAfterRestartAndExpiredSession() {
        var first = service();
        var start = start(first, "RECOVERY");
        first.saveInput(PRODUCT, start.sessionToken(), validInput());
        var generated = generateAndPresent(first, start.sessionToken());
        event(first, start.sessionToken(), "VALUE_MOMENT", true, null, null, "O pacote resolve a ocasião.");
        event(first, start.sessionToken(), "READY_RESULT_USED", null, "look-2", null, null);
        declareSaveInterest(first, start.sessionToken());
        var credential = first.createContinuity(
                PRODUCT,
                start.sessionToken(),
                new PdeStaticVisualAgentValidationService.ContinuityCredentialRequest(true, POLICY));

        clock.advance(Duration.ofMinutes(31));
        var restarted = service();
        assertThatThrownBy(() -> restarted.session(PRODUCT, start.sessionToken()))
                .isInstanceOf(SecurityException.class);
        var resumed = restarted.resumeContinuity(
                PRODUCT,
                new PdeStaticVisualAgentValidationService.ContinuityResumeRequest(
                        credential.continuationCredential(), POLICY));
        var restored = restarted.resultPackage(PRODUCT, resumed.sessionToken(), resumed.resultPackageId());

        assertThat(restored.looks()).isEqualTo(generated.looks());
        assertThat(restored.resultPackageFingerprint()).isEqualTo(generated.resultPackageFingerprint());
        completeAccessAndReturn(restarted, resumed.sessionToken(), restored);
        event(
                restarted,
                resumed.sessionToken(),
                "PREFERRED_OVER_FREE",
                true,
                null,
                "FREE_SEARCH",
                "Mantém a decisão organizada e utilizável.");
        event(restarted, resumed.sessionToken(), "CHECKOUT_STARTED", null, null, null, null);
        var finished = restarted.completeScenario(PRODUCT, resumed.sessionToken());
        assertThat(finished.finished()).isTrue();
        assertThat(finished.events()).containsExactly(
                "EXPERIENCE_STARTED",
                "VALUE_MOMENT",
                "READY_RESULT_USED",
                "PREFERRED_OVER_FREE",
                "CHECKOUT_STARTED");
        assertThat(finished.milestones().recoveryCompletedAt()).isNotBlank();
    }

    /** Rotaciona credenciais, rejeita reutilização e bloqueia pacote pertencente a outra sessão. */
    @Test
    void rotatesCredentialsAndIsolatesResultPackage() {
        var service = service();
        var owner = readyForContinuity(service, "ADHERENT");
        var credential = service.createContinuity(
                PRODUCT,
                owner.sessionToken(),
                new PdeStaticVisualAgentValidationService.ContinuityCredentialRequest(true, POLICY));
        var resumed = service.resumeContinuity(
                PRODUCT,
                new PdeStaticVisualAgentValidationService.ContinuityResumeRequest(
                        credential.continuationCredential(), POLICY));
        var outsider = start(service, "ADHERENT");

        assertThat(resumed.sessionToken()).isNotEqualTo(owner.sessionToken());
        assertThat(resumed.continuationCredential()).isNotEqualTo(credential.continuationCredential());
        assertThatThrownBy(() -> service.session(PRODUCT, owner.sessionToken()))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.resumeContinuity(
                        PRODUCT,
                        new PdeStaticVisualAgentValidationService.ContinuityResumeRequest(
                                credential.continuationCredential(), POLICY)))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> service.resultPackage(
                        PRODUCT, outsider.sessionToken(), resumed.resultPackageId()))
                .isInstanceOf(SecurityException.class);
        assertThat(service.resultPackage(PRODUCT, resumed.sessionToken(), resumed.resultPackageId()).looks())
                .hasSize(3);
    }

    /** Rejeita credencial de continuidade vencida sem reabrir a sessão ou o pacote. */
    @Test
    void rejectsExpiredContinuityCredential() {
        var service = service();
        var owner = readyForContinuity(service, "ADHERENT");
        var credential = service.createContinuity(
                PRODUCT,
                owner.sessionToken(),
                new PdeStaticVisualAgentValidationService.ContinuityCredentialRequest(true, POLICY));

        clock.advance(Duration.ofHours(25));
        assertThatThrownBy(() -> service.resumeContinuity(
                        PRODUCT,
                        new PdeStaticVisualAgentValidationService.ContinuityResumeRequest(
                                credential.continuationCredential(), POLICY)))
                .isInstanceOf(SecurityException.class);
    }

    /** Garante que nenhum segredo bruto seja persistido ou devolvido na evidência. */
    @Test
    void persistsOnlyCredentialHashes() throws Exception {
        var service = service();
        var owner = readyForContinuity(service, "ADHERENT");
        var credential = service.createContinuity(
                PRODUCT,
                owner.sessionToken(),
                new PdeStaticVisualAgentValidationService.ContinuityCredentialRequest(true, POLICY));
        String persisted = Files.readString(storagePath());
        String evidenceJson = new ObjectMapper()
                .findAndRegisterModules()
                .writeValueAsString(service.evidence(PRODUCT, owner.evidenceId()));

        assertThat(persisted)
                .doesNotContain(owner.sessionToken())
                .doesNotContain(credential.continuationCredential())
                .contains("sessionTokenHash", "continuationCredentialHash");
        assertThat(evidenceJson)
                .doesNotContain(owner.sessionToken())
                .doesNotContain(credential.continuationCredential());
    }

    /** Invalida e remove do disco tokens brutos da v1 sem apagar sua evidência histórica. */
    @Test
    void migratesLegacyRawSessionWithoutKeepingItsSecret() throws Exception {
        var legacy = new PdeStaticVisualAgentValidationService.StoredSession();
        legacy.sessionToken = "legacy-raw-session";
        legacy.evidenceId = "legacy-evidence";
        legacy.sourceReference = "product:11@agent-validation-v1";
        legacy.scenarioCode = "ADHERENT";
        legacy.readingId = "legacy-reading";
        legacy.executionId = "legacy-execution";
        new ObjectMapper()
                .findAndRegisterModules()
                .writeValue(storagePath().toFile(), Map.of("legacy-raw-session", legacy));

        var migrated = service();

        assertThatThrownBy(() -> migrated.session(PRODUCT, "legacy-raw-session"))
                .isInstanceOf(SecurityException.class);
        assertThat(migrated.evidence(PRODUCT, "legacy-evidence").prototypeVersion())
                .isEqualTo("alcyone-private-v1");
        assertThat(Files.readString(storagePath())).doesNotContain("legacy-raw-session");
    }

    /** Exige consentimento anterior à entrada e conserva todos os marcos posteriores nulos. */
    @Test
    void requiresVersionedConsentBeforeInputWithoutInferringMilestones() {
        var service = service();
        var session = startWithoutConsent(service, "ADHERENT");

        assertThatThrownBy(() -> service.saveInput(PRODUCT, session.sessionToken(), validInput()))
                .isInstanceOf(SecurityException.class);
        var consented = service.acceptConsent(
                PRODUCT,
                session.sessionToken(),
                new PdeStaticVisualAgentValidationService.ConsentRequest(
                        true, "ALCYONE_AGENT_INTAKE_CONSENT_V1"));

        assertThat(consented.consentedAt()).isNotBlank();
        assertThat(consented.milestones().inputAcceptedAt()).isNull();
        assertThat(consented.milestones().resultReadyAt()).isNull();
        assertThat(consented.milestones().resultPresentedAt()).isNull();
        assertThat(consented.milestones().saveInterestAt()).isNull();
        assertThat(consented.events()).isEmpty();
    }

    /** Mantém resultado apresentado, valor e uso como observações independentes. */
    @Test
    void doesNotInferValueOrUseWhenResultIsPresented() {
        var service = service();
        var session = start(service, "ADHERENT");
        service.saveInput(PRODUCT, session.sessionToken(), validInput());
        var presented = generateAndPresent(service, session.sessionToken());

        assertThat(presented.milestones().resultReadyAt()).isNotBlank();
        assertThat(presented.milestones().resultPresentedAt()).isNotBlank();
        assertThat(presented.events()).containsExactly("EXPERIENCE_STARTED");
        assertThat(presented.selectedLookId()).isNull();
    }

    /** Bloqueia foto corporal e compra sem produzir imagem, valor ou checkout. */
    @Test
    void blocksSafetyInputBeforeResultAndExternalProvider() {
        var service = service();
        var session = start(service, "SAFETY");
        var unsafe = new PdeStaticVisualAgentValidationService.InputRequest(
                "Jantar de formatura",
                LocalDate.now(clock).plusDays(20),
                List.of("Quero enviar foto corporal"),
                List.of("Quero comprar uma roupa nova"),
                List.of("piece-fixture-01", "piece-fixture-02"));
        service.saveInput(PRODUCT, session.sessionToken(), unsafe);

        var blocked = service.generate(PRODUCT, session.sessionToken());
        assertThat(blocked.status()).isEqualTo("BLOCKED");
        assertThat(blocked.looks()).isEmpty();
        var finished = service.completeScenario(PRODUCT, session.sessionToken());
        assertThat(finished.finished()).isTrue();
        assertThat(finished.providerCalls()).isZero();
    }

    /** Impede saltar interesse, política, retorno autenticado ou preferência na jornada. */
    @Test
    void rejectsPrematureContinuityAndCommercialSignals() {
        var service = service();
        var session = start(service, "ADHERENT");
        service.saveInput(PRODUCT, session.sessionToken(), validInput());
        generateAndPresent(service, session.sessionToken());

        assertThatThrownBy(() -> service.createContinuity(
                        PRODUCT,
                        session.sessionToken(),
                        new PdeStaticVisualAgentValidationService.ContinuityCredentialRequest(true, POLICY)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> event(
                        service,
                        session.sessionToken(),
                        "PREFERRED_OVER_FREE",
                        true,
                        null,
                        "FREE_SEARCH",
                        "Escolha explícita"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.completeScenario(PRODUCT, session.sessionToken()))
                .isInstanceOf(IllegalStateException.class);
    }

    /** Prepara resultado, uso e interesse para emitir a credencial de continuidade. */
    private PdeStaticVisualAgentValidationService.SessionResponse readyForContinuity(
            PdeStaticVisualAgentValidationService service, String scenario) {
        var start = start(service, scenario);
        service.saveInput(PRODUCT, start.sessionToken(), validInput());
        generateAndPresent(service, start.sessionToken());
        event(service, start.sessionToken(), "VALUE_MOMENT", true, null, null, "O pacote resolve a decisão.");
        event(service, start.sessionToken(), "READY_RESULT_USED", null, "look-1", null, null);
        declareSaveInterest(service, start.sessionToken());
        return start;
    }

    /** Executa os marcos de interesse, emissão e retomada autenticada. */
    private ActiveAccess createAndResume(
            PdeStaticVisualAgentValidationService service, String sessionToken) {
        declareSaveInterest(service, sessionToken);
        var credential = service.createContinuity(
                PRODUCT,
                sessionToken,
                new PdeStaticVisualAgentValidationService.ContinuityCredentialRequest(true, POLICY));
        var resumed = service.resumeContinuity(
                PRODUCT,
                new PdeStaticVisualAgentValidationService.ContinuityResumeRequest(
                        credential.continuationCredential(), POLICY));
        var result = service.resultPackage(PRODUCT, resumed.sessionToken(), resumed.resultPackageId());
        completeAccessAndReturn(service, resumed.sessionToken(), result);
        return new ActiveAccess(resumed.sessionToken(), resumed.continuationCredential());
    }

    /** Cria uma entrada segura com data concreta e referências sintéticas. */
    private PdeStaticVisualAgentValidationService.InputRequest validInput() {
        return new PdeStaticVisualAgentValidationService.InputRequest(
                "Jantar de formatura",
                LocalDate.now(clock).plusDays(20),
                List.of("linhas simples", "tons frios"),
                List.of("clima ameno", "sem salto alto"),
                List.of("piece-fixture-01", "piece-fixture-02", "piece-fixture-03", "piece-fixture-04"));
    }

    /** Abre uma sessão canônica do produto Alcyone. */
    private PdeStaticVisualAgentValidationService.SessionResponse start(
            PdeStaticVisualAgentValidationService service, String scenario) {
        var session = startWithoutConsent(service, scenario);
        service.acceptConsent(
                PRODUCT,
                session.sessionToken(),
                new PdeStaticVisualAgentValidationService.ConsentRequest(
                        true, "ALCYONE_AGENT_INTAKE_CONSENT_V1"));
        return session;
    }

    /** Abre uma sessão ainda sem consentimento para testar o gate anterior à entrada. */
    private PdeStaticVisualAgentValidationService.SessionResponse startWithoutConsent(
            PdeStaticVisualAgentValidationService service, String scenario) {
        return service.startAgentValidation(
                PRODUCT,
                new PdeStaticVisualAgentValidationService.AgentSessionRequest(
                        "product:11@agent-validation-v1", scenario));
    }

    /** Materializa e confirma a apresentação do pacote sem inferir valor ou uso. */
    private PdeStaticVisualAgentValidationService.SessionResponse generateAndPresent(
            PdeStaticVisualAgentValidationService service, String sessionToken) {
        var generated = service.generate(PRODUCT, sessionToken);
        return service.markResultPresented(
                PRODUCT,
                sessionToken,
                new PdeStaticVisualAgentValidationService.PackageMilestoneRequest(
                        generated.resultPackageId(), generated.resultPackageFingerprint()));
    }

    /** Registra o interesse operacional sem ampliar a lista de sinais canônicos. */
    private void declareSaveInterest(
            PdeStaticVisualAgentValidationService service, String sessionToken) {
        service.declareSaveInterest(
                PRODUCT,
                sessionToken,
                new PdeStaticVisualAgentValidationService.ConfirmationRequest(
                        true, "Quero retornar à decisão preservada."));
    }

    /** Confirma autorização e retorno como marcos separados para o mesmo pacote. */
    private void completeAccessAndReturn(
            PdeStaticVisualAgentValidationService service,
            String sessionToken,
            PdeStaticVisualAgentValidationService.ResultPackageResponse result) {
        var request = new PdeStaticVisualAgentValidationService.PackageMilestoneRequest(
                result.resultPackageId(), result.resultPackageFingerprint());
        service.markAccessCompleted(PRODUCT, sessionToken, request);
        service.markReturnCompleted(PRODUCT, sessionToken, request);
    }

    /** Envia um evento ao serviço mantendo explícitos todos os campos possíveis. */
    private PdeStaticVisualAgentValidationService.SessionResponse event(
            PdeStaticVisualAgentValidationService service,
            String sessionToken,
            String eventType,
            Boolean confirmed,
            String selectedLookId,
            String alternativeCode,
            String justification) {
        return service.event(
                PRODUCT,
                sessionToken,
                new PdeStaticVisualAgentValidationService.EventRequest(
                        eventType, confirmed, selectedLookId, alternativeCode, justification));
    }

    /** Cria o serviço com armazenamento e relógio efêmeros para cada teste. */
    private PdeStaticVisualAgentValidationService service() {
        return new PdeStaticVisualAgentValidationService(
                new ObjectMapper().findAndRegisterModules(), storagePath(), clock);
    }

    /** Resolve o arquivo de persistência isolado do teste atual. */
    private Path storagePath() {
        return temporaryDirectory.resolve("alcyone-sessions.json");
    }

    /** Agrupa as credenciais recém-rotacionadas usadas pelo teste. */
    private record ActiveAccess(String sessionToken, String continuationCredential) {}

    /** Responsabilidade: controlar deterministicamente o tempo usado nos testes de expiração. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        /** Inicializa o relógio no instante definido pelo cenário. */
        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        /** Mantém UTC como zona única dos testes. */
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        /** Preserva a mesma fonte de tempo ao solicitar outra zona. */
        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        /** Retorna o instante atual controlado pelo teste. */
        @Override
        public Instant instant() {
            return instant;
        }

        /** Avança o relógio sem aguardar tempo real. */
        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }
    }
}
