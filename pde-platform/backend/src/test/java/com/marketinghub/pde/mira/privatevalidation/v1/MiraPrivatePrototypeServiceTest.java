package com.marketinghub.pde.mira.privatevalidation.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.service.AccessService;
import java.nio.file.Path;
import java.util.List;
import org.mockito.ArgumentCaptor;
import com.marketinghub.pde.dto.FunnelEventRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Valida o protótipo privado de Mira sem contato, publicação, cobrança ou mídia. */
class MiraPrivatePrototypeServiceTest {
    @TempDir Path temporaryDirectory;

    /** Comprova o caminho feliz e os cinco eventos idempotentes de uma sessão interna. */
    @Test
    void completesQaJourneyWithoutCommercialSideEffects() {
        AccessService access = mock(AccessService.class);
        MiraPrivatePrototypeService service = service(access);

        var started = service.access(new MiraPrivatePrototypeService.AccessRequest("qa-secret", true));
        service.saveInput(started.sessionToken(), new MiraPrivatePrototypeService.InputRequest(
                "45-54", "Organizar os produtos que já possuo", List.of(
                new MiraPrivatePrototypeService.ProductInput("Hidratante Brisa", "Aplicar após a limpeza"),
                new MiraPrivatePrototypeService.ProductInput("Limpador Sereno", "Usar para limpar e enxaguar"))));
        var ready = service.generate(started.sessionToken());
        service.generate(started.sessionToken());
        service.event(started.sessionToken(), new MiraPrivatePrototypeService.EventRequest("READY_RESULT_USED", true));
        service.event(started.sessionToken(), new MiraPrivatePrototypeService.EventRequest("PREFERRED_OVER_FREE", true));
        var completed = service.event(started.sessionToken(), new MiraPrivatePrototypeService.EventRequest("CHECKOUT_STARTED", true));

        assertThat(ready.status()).isEqualTo("READY");
        assertThat(ready.routine()).extracting(MiraPrivatePrototypeService.RoutineCard::productName)
                .containsExactly("Limpador Sereno", "Hidratante Brisa");
        assertThat(completed.events()).containsExactlyInAnyOrder(
                "EXPERIENCE_STARTED", "VALUE_MOMENT", "READY_RESULT_USED", "PREFERRED_OVER_FREE", "CHECKOUT_STARTED");
        assertThat(completed.checkoutMode()).isEqualTo("SIMULATED_NO_CHARGE");
        assertThat(completed.trafficClass()).isEqualTo("QA_INTERNAL");
        verify(access, times(5)).recordFunnelEvent(any());
    }

    /** Bloqueia objetivo clínico sem apresentar rotina ou momento de valor. */
    @Test
    void blocksClinicalObjective() {
        MiraPrivatePrototypeService service = service(mock(AccessService.class));
        var started = service.access(new MiraPrivatePrototypeService.AccessRequest("qa-secret", true));
        service.saveInput(started.sessionToken(), new MiraPrivatePrototypeService.InputRequest(
                "45-54", "Diagnosticar e tratar manchas", List.of(
                new MiraPrivatePrototypeService.ProductInput("Limpador", "Limpar e enxaguar"))));

        var blocked = service.generate(started.sessionToken());

        assertThat(blocked.status()).isEqualTo("BLOCKED");
        assertThat(blocked.routine()).isEmpty();
        assertThat(blocked.blocker()).contains("conclusão clínica");
        assertThat(blocked.events()).doesNotContain("VALUE_MOMENT");
    }

    /** Rejeita acesso inexistente e checkout antecipado. */
    @Test
    void rejectsInvalidAccessAndPrematureCheckout() {
        MiraPrivatePrototypeService service = service(mock(AccessService.class));
        assertThatThrownBy(() -> service.access(new MiraPrivatePrototypeService.AccessRequest("wrong", true)))
                .isInstanceOf(SecurityException.class);
        var started = service.access(new MiraPrivatePrototypeService.AccessRequest("qa-secret", true));
        assertThatThrownBy(() -> service.event(started.sessionToken(),
                new MiraPrivatePrototypeService.EventRequest("CHECKOUT_STARTED", true)))
                .isInstanceOf(IllegalStateException.class);
    }

    /** Comprova retomada após reconstrução do serviço usando o mesmo arquivo. */
    @Test
    void resumesPersistedSession() {
        MiraPrivatePrototypeService first = service(mock(AccessService.class));
        var started = first.access(new MiraPrivatePrototypeService.AccessRequest("qa-secret", true));
        MiraPrivatePrototypeService restarted = service(mock(AccessService.class));

        assertThat(restarted.session(started.sessionToken()).participantReference()).isEqualTo("QA-MIRA-LOCAL");
        assertThat(restarted.session(started.sessionToken()).events()).containsExactly("EXPERIENCE_STARTED");
    }

    /** Mantém QA separado dos dois convites humanos, inclusive na projeção administrativa. */
    @Test
    void neverImportsQaAsHumanReading() {
        MiraPrivatePrototypeService service = service(mock(AccessService.class));
        service.access(new MiraPrivatePrototypeService.AccessRequest("qa-secret", true));
        assertThat(service.readingEvidence(1).trafficClass()).isEqualTo("NOT_STARTED");
        assertThat(service.readingEvidence(1).signals().values()).containsOnly(false);
        var first = service.access(new MiraPrivatePrototypeService.AccessRequest("participant-one", true));
        var second = service.access(new MiraPrivatePrototypeService.AccessRequest("participant-two", true));
        assertThat(first.sessionToken()).isNotEqualTo(second.sessionToken());
        assertThat(service.readingEvidence(1).participantReference()).isEqualTo(first.participantReference());
        assertThat(service.readingEvidence(2).participantReference()).isEqualTo(second.participantReference());
        assertThatThrownBy(() -> service.readingEvidence(3)).isInstanceOf(IllegalArgumentException.class);
    }

    /** Não cria eventos nem sessão de participante sem consentimento explícito. */
    @Test
    void requiresConsentBeforeAnyEvidence() {
        AccessService access = mock(AccessService.class);
        MiraPrivatePrototypeService service = service(access);
        assertThatThrownBy(() -> service.access(new MiraPrivatePrototypeService.AccessRequest("participant-one", false)))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(access);
        assertThat(service.readingEvidence(1).consentedAt()).isNull();
    }

    /** Preserva uma negativa encerrada após reinício, sem fabricar preferência ou checkout. */
    @Test
    void persistsNegativeReadingAndPreventsRewritingIt() {
        MiraPrivatePrototypeService service = service(mock(AccessService.class));
        var started = service.access(new MiraPrivatePrototypeService.AccessRequest("participant-one", true));
        var finished = service.finish(started.sessionToken());
        var evidence = service.readingEvidence(1);
        assertThat(finished.readingFinished()).isTrue();
        assertThat(evidence.finishedAt()).isNotNull();
        assertThat(evidence.signals()).containsEntry("PREFERRED_OVER_FREE", false).containsEntry("CHECKOUT_STARTED", false);
        assertThat(service(mock(AccessService.class)).readingEvidence(1)).isEqualTo(evidence);
        assertThatThrownBy(() -> service.saveInput(started.sessionToken(), new MiraPrivatePrototypeService.InputRequest(
                "45-54", "Organizar rotina", List.of(new MiraPrivatePrototypeService.ProductInput("Limpeza", "Limpar e enxaguar")))))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.generate(started.sessionToken())).isInstanceOf(IllegalStateException.class);
    }

    /** Impede usar eventos da rotina anterior como evidência de uma entrada diferente. */
    @Test
    void freezesInputAfterValueMomentAndRequiresUseBeforePreference() {
        MiraPrivatePrototypeService service = service(mock(AccessService.class));
        var started = service.access(new MiraPrivatePrototypeService.AccessRequest("participant-one", true));
        var input = new MiraPrivatePrototypeService.InputRequest("45-54", "Organizar rotina",
                List.of(new MiraPrivatePrototypeService.ProductInput("Limpeza", "Limpar e enxaguar")));
        service.saveInput(started.sessionToken(), input);
        service.generate(started.sessionToken());
        assertThat(service.saveInput(started.sessionToken(), input).status()).isEqualTo("READY");
        var different = new MiraPrivatePrototypeService.InputRequest("45-54", "Objetivo diferente", input.products());
        assertThatThrownBy(() -> service.saveInput(started.sessionToken(), different)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.event(started.sessionToken(),
                new MiraPrivatePrototypeService.EventRequest("PREFERRED_OVER_FREE", true))).isInstanceOf(IllegalStateException.class);
    }

    /** Mantém a credencial de sessão fora dos eventos e da prova administrativa. */
    @Test
    void doesNotExposeSessionCredentialsInEvidenceOrAnalytics() throws Exception {
        AccessService access = mock(AccessService.class);
        MiraPrivatePrototypeService service = service(access);
        var started = service.access(new MiraPrivatePrototypeService.AccessRequest("participant-one", true));
        var event = ArgumentCaptor.forClass(FunnelEventRequest.class);
        verify(access).recordFunnelEvent(event.capture());
        var json = new ObjectMapper();
        assertThat(json.writeValueAsString(event.getValue())).doesNotContain(started.sessionToken(), "participant-one");
        assertThat(json.writeValueAsString(service.readingEvidence(1))).doesNotContain(started.sessionToken(), "participant-one");
    }

    /** Cria o serviço com acessos separados e armazenamento temporário. */
    private MiraPrivatePrototypeService service(AccessService accessService) {
        return new MiraPrivatePrototypeService(accessService, new ObjectMapper(),
                temporaryDirectory.resolve("sessions.json").toString(), "participant-one", "participant-two", "qa-secret");
    }
}
