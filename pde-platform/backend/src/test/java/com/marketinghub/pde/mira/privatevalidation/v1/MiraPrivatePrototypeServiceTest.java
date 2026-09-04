package com.marketinghub.pde.mira.privatevalidation.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.pde.service.AccessService;
import java.nio.file.Path;
import java.util.List;
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

    /** Cria o serviço com acessos separados e armazenamento temporário. */
    private MiraPrivatePrototypeService service(AccessService accessService) {
        return new MiraPrivatePrototypeService(accessService, new ObjectMapper(),
                temporaryDirectory.resolve("sessions.json").toString(), "participant-one", "participant-two", "qa-secret");
    }
}
