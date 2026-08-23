package com.marketinghub.pde.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.marketinghub.pde.service.AiGuidanceService;
import com.marketinghub.pde.service.InternalApiAuthorizer;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Valida que somente o executor autenticado consome ou conclui pendências de IA do PDE. */
class AiGuidanceControllerSecurityTest {

    /** Permite consultar a fila interna quando a credencial coincide. */
    @Test
    void allowsPendingQueueForAuthenticatedWorker() {
        AiGuidanceService service = mock(AiGuidanceService.class);
        when(service.getPendingGuidance()).thenReturn(Optional.empty());
        AiGuidanceController controller =
                new AiGuidanceController(service, new InternalApiAuthorizer("segredo-interno"));

        assertThat(controller.getPendingGuidance("segredo-interno")).isEmpty();
    }

    /** Bloqueia leitura da fila antes de acessar o serviço quando falta credencial. */
    @Test
    void rejectsPendingQueueWithoutWorkerCredential() {
        AiGuidanceService service = mock(AiGuidanceService.class);
        AiGuidanceController controller =
                new AiGuidanceController(service, new InternalApiAuthorizer("segredo-interno"));

        assertThatThrownBy(() -> controller.getPendingGuidance(null))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(service);
    }
}
