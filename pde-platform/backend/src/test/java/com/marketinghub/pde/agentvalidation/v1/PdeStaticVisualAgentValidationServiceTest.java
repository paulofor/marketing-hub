package com.marketinghub.pde.agentvalidation.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Responsabilidade: comprovar o protótipo visual sintético sem evidência humana ou efeito externo. */
class PdeStaticVisualAgentValidationServiceTest {
    private static final String PRODUCT = "pde-planejado-46";
    @TempDir Path temporaryDirectory;

    /** Conclui os cinco sinais com três imagens estáticas e custo externo zero. */
    @Test
    void completesAdherentScenarioWithStaticFixturesOnly() {
        var service = service();
        var session = start(service, "ADHERENT");

        var inputReady = service.saveInput(PRODUCT, session.sessionToken(), validInput());
        var generated = service.generate(PRODUCT, session.sessionToken());
        service.generate(PRODUCT, session.sessionToken());
        event(service, session, "VALUE_MOMENT", true, null, null, "As três opções resolvem a decisão.");
        event(service, session, "READY_RESULT_USED", null, "look-1", null, null);
        event(service, session, "PREFERRED_OVER_FREE", true, null, "FREE_SEARCH", "É mais direto que pesquisar referências soltas.");
        event(service, session, "CHECKOUT_STARTED", null, null, null, null);
        var finished = event(service, session, "AGENT_SCENARIO_COMPLETED", null, null, null, null);
        var evidence = service.evidence(PRODUCT, session.evidenceId());

        assertThat(inputReady.events()).containsExactly("EXPERIENCE_STARTED");
        assertThat(generated.looks()).hasSize(3).allMatch(look -> look.imagePath().endsWith(".png"));
        assertThat(generated.events()).doesNotContain("VALUE_MOMENT", "READY_RESULT_USED");
        assertThat(finished.finished()).isTrue();
        assertThat(evidence.events()).containsExactly(
                "EXPERIENCE_STARTED",
                "VALUE_MOMENT",
                "READY_RESULT_USED",
                "PREFERRED_OVER_FREE",
                "CHECKOUT_STARTED",
                "AGENT_SCENARIO_COMPLETED");
        assertThat(evidence.fixtureContract()).isEqualTo("PDE_STATIC_RESULT_FIXTURES_V1");
        assertThat(evidence.providerCalls()).isZero();
        assertThat(evidence.providerCostUsd()).isZero();
        assertThat(evidence.sideEffects())
                .containsEntry("paymentEnabled", false)
                .containsEntry("published", false)
                .containsEntry("campaignCreated", false)
                .containsEntry("mediaSpendBrl", 0);
        assertThat(evidence.humanEvidenceClaimed()).isFalse();
        assertThat(evidence.commercialEvidenceClaimed()).isFalse();
        assertThat(evidence.eventAudit())
                .hasSize(6)
                .allMatch(event -> "AGENT_VALIDATION".equals(event.metadata().get("trafficClass")))
                .allMatch(event -> Boolean.TRUE.equals(event.metadata().get("mh_internal_test")));
    }

    /** Recupera exatamente a mesma entrada e o mesmo pacote depois de reiniciar o serviço. */
    @Test
    void resumesRecoveryWithoutRegenerationOrDuplicateEvents() {
        var first = service();
        var session = start(first, "RECOVERY");
        first.saveInput(PRODUCT, session.sessionToken(), validInput());
        var packageBefore = first.generate(PRODUCT, session.sessionToken());

        var restarted = service();
        var packageAfter = restarted.session(PRODUCT, session.sessionToken());
        assertThat(packageAfter.looks()).isEqualTo(packageBefore.looks());
        assertThat(packageAfter.resultPackageFingerprint())
                .isEqualTo(packageBefore.resultPackageFingerprint());
        assertThat(packageAfter.events()).containsExactly("EXPERIENCE_STARTED");

        event(restarted, session, "VALUE_MOMENT", true, null, null, "O pacote resolve a ocasião.");
        event(restarted, session, "READY_RESULT_USED", null, "look-2", null, null);
        event(restarted, session, "PREFERRED_OVER_FREE", true, null, "FREE_SEARCH", "Mantém a decisão organizada e utilizável.");
        event(restarted, session, "CHECKOUT_STARTED", null, null, null, null);
        event(restarted, session, "RECOVERY_COMPLETED", null, null, null, null);
        var finished = event(restarted, session, "AGENT_SCENARIO_COMPLETED", null, null, null, null);

        assertThat(finished.finished()).isTrue();
        assertThat(finished.events()).contains("RECOVERY_COMPLETED");
        assertThat(finished.events()).doesNotHaveDuplicates();
    }

    /** Bloqueia foto corporal e compra sem produzir imagem, valor ou checkout. */
    @Test
    void blocksSafetyInputBeforeResultAndExternalProvider() {
        var service = service();
        var session = start(service, "SAFETY");
        var unsafe = new PdeStaticVisualAgentValidationService.InputRequest(
                "Jantar de formatura",
                LocalDate.now().plusDays(20),
                List.of("Quero enviar foto corporal"),
                List.of("Quero comprar uma roupa nova"),
                List.of("piece-fixture-01", "piece-fixture-02"));
        service.saveInput(PRODUCT, session.sessionToken(), unsafe);

        var blocked = service.generate(PRODUCT, session.sessionToken());
        assertThat(blocked.status()).isEqualTo("BLOCKED");
        assertThat(blocked.looks()).isEmpty();
        assertThat(blocked.blocker()).contains("foto corporal", "compra");
        event(service, session, "SAFETY_LIMIT_BLOCKED", null, null, null, null);
        var finished = event(service, session, "AGENT_SCENARIO_COMPLETED", null, null, null, null);

        assertThat(finished.finished()).isTrue();
        assertThat(finished.events())
                .containsExactly(
                        "EXPERIENCE_STARTED", "SAFETY_LIMIT_BLOCKED", "AGENT_SCENARIO_COMPLETED")
                .doesNotContain("VALUE_MOMENT", "CHECKOUT_STARTED");
        assertThat(finished.providerCalls()).isZero();
    }

    /** Impede inferir preferência, checkout, recuperação ou conclusão fora de ordem. */
    @Test
    void rejectsPrematureAndCrossScenarioEvidence() {
        var service = service();
        var session = start(service, "ADHERENT");
        service.saveInput(PRODUCT, session.sessionToken(), validInput());
        service.generate(PRODUCT, session.sessionToken());

        assertThatThrownBy(() -> event(service, session, "READY_RESULT_USED", null, "look-1", null, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> event(service, session, "PREFERRED_OVER_FREE", true, null, "FREE_SEARCH", "Escolha explícita"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> event(service, session, "CHECKOUT_STARTED", null, null, null, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> event(service, session, "RECOVERY_COMPLETED", null, null, null, null))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> event(service, session, "AGENT_SCENARIO_COMPLETED", null, null, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    /** Mantém tokens e entradas de uma sessão inacessíveis por outra sessão. */
    @Test
    void isolatesSessionsAndRejectsWrongProductIdentity() throws Exception {
        var service = service();
        var first = start(service, "ADHERENT");
        var second = start(service, "ADHERENT");
        service.saveInput(PRODUCT, first.sessionToken(), validInput());

        assertThat(service.session(PRODUCT, second.sessionToken()).input()).isNull();
        assertThat(first.sessionToken()).isNotEqualTo(second.sessionToken());
        assertThat(service.evidence(PRODUCT, first.evidenceId()).toString())
                .doesNotContain(first.sessionToken(), second.sessionToken());
        assertThatThrownBy(() -> service.session("another-product", first.sessionToken()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.session(PRODUCT, "unknown"))
                .isInstanceOf(SecurityException.class);
        assertThat(new ObjectMapper()
                        .findAndRegisterModules()
                        .writeValueAsString(service.evidence(PRODUCT, first.evidenceId())))
                .doesNotContain(first.sessionToken());
    }

    /** Cria uma entrada segura com data concreta e referências sintéticas. */
    private PdeStaticVisualAgentValidationService.InputRequest validInput() {
        return new PdeStaticVisualAgentValidationService.InputRequest(
                "Jantar de formatura",
                LocalDate.now().plusDays(20),
                List.of("linhas simples", "tons frios"),
                List.of("clima ameno", "sem salto alto"),
                List.of("piece-fixture-01", "piece-fixture-02", "piece-fixture-03", "piece-fixture-04"));
    }

    /** Abre uma sessão canônica do produto Alcyone. */
    private PdeStaticVisualAgentValidationService.SessionResponse start(
            PdeStaticVisualAgentValidationService service, String scenario) {
        return service.startAgentValidation(
                PRODUCT,
                new PdeStaticVisualAgentValidationService.AgentSessionRequest(
                        "product:11@agent-validation-v1", scenario));
    }

    /** Envia um evento ao serviço mantendo explícitos todos os campos possíveis. */
    private PdeStaticVisualAgentValidationService.SessionResponse event(
            PdeStaticVisualAgentValidationService service,
            PdeStaticVisualAgentValidationService.SessionResponse session,
            String eventType,
            Boolean confirmed,
            String selectedLookId,
            String alternativeCode,
            String justification) {
        return service.event(
                PRODUCT,
                session.sessionToken(),
                new PdeStaticVisualAgentValidationService.EventRequest(
                        eventType, confirmed, selectedLookId, alternativeCode, justification));
    }

    /** Cria o serviço com armazenamento efêmero para cada teste. */
    private PdeStaticVisualAgentValidationService service() {
        return new PdeStaticVisualAgentValidationService(
                new ObjectMapper().findAndRegisterModules(),
                temporaryDirectory.resolve("alcyone-sessions.json").toString());
    }
}
