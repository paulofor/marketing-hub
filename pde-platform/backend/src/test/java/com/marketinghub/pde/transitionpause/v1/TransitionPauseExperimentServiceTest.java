package com.marketinghub.pde.transitionpause.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.dto.FunnelEventResponse;
import com.marketinghub.pde.service.AccessService;
import org.junit.jupiter.api.Test;

/** Valida os gates de consentimento, randomização e eventos da Pausa de Transição. */
class TransitionPauseExperimentServiceTest {

    /** Confirma que uma sessão recebe sempre a mesma variante e registra consentimento auditável. */
    @Test
    void startsSessionWithStableVariantAndConsentEvents() {
        AccessService accessService = mock(AccessService.class);
        when(accessService.recordFunnelEvent(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new FunnelEventResponse("event", "TYPE", "RECORDED"));
        TransitionPauseExperimentService service = new TransitionPauseExperimentService(accessService);
        TransitionPauseSessionRequest request =
                new TransitionPauseSessionRequest("participant-1", "session-1", "Organizar documentos", true, true, true);

        TransitionPauseSessionResponse first = service.startSession(request);
        TransitionPauseSessionResponse second = service.startSession(request);

        assertEquals(first.variant(), second.variant());
        verify(accessService, times(2)).recordFunnelEvent(argThat(event ->
                event.eventType().equals("EXPERIMENT_CONSENT_RECORDED")
                        && Boolean.TRUE.equals(event.metadata().get("consentAccepted"))));
        verify(accessService, times(2)).recordFunnelEvent(argThat(event ->
                event.eventType().equals("EXPERIMENT_SESSION_STARTED")
                        && Boolean.FALSE.equals(event.metadata().get("paidTraffic"))));
    }

    /** Confirma que somente eventos humanos previstos no protocolo podem ser persistidos. */
    @Test
    void rejectsEventsOutsideVersionedProtocol() {
        TransitionPauseExperimentService service = new TransitionPauseExperimentService(mock(AccessService.class));
        TransitionPauseEventRequest request = new TransitionPauseEventRequest(
                "participant-1", "session-1", "PURCHASED", 8, 4, 30, true, null);

        assertThrows(IllegalArgumentException.class, () -> service.recordOutcome(request));
    }

    /** Confirma que o desfecho preserva métricas humanas e a versão do experimento. */
    @Test
    void recordsHumanOutcomeWithComparableMetrics() {
        AccessService accessService = mock(AccessService.class);
        when(accessService.recordFunnelEvent(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new FunnelEventResponse("event", "TRANSITION_PAUSE_TASK_STARTED", "RECORDED"));
        TransitionPauseExperimentService service = new TransitionPauseExperimentService(accessService);

        service.recordOutcome(new TransitionPauseEventRequest(
                "participant-1", "session-1", "TASK_STARTED", 8, 4, 42, false, null));

        verify(accessService).recordFunnelEvent(argThat(event ->
                event.eventType().equals("TRANSITION_PAUSE_TASK_STARTED")
                        && event.metadata().get("secondsUntilTaskStarted").equals(42)
                        && event.metadata().get("experienceVersion").equals("pausa-de-transicao-v1")
                        && Boolean.TRUE.equals(event.metadata().get("humanReported"))));
    }
}
