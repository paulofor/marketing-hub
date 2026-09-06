package com.marketinghub.pde.mira.privatevalidation.v1;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: impedir consulta não autenticada da evidência privada. */
class MiraPrivatePrototypeControllerTest {
    /** Exige o segredo interno antes de consultar qualquer participante ou estado de leitura. */
    @Test
    void protectsEvidenceEvenForKnownReadingNumbers() {
        var service = mock(MiraPrivatePrototypeService.class);
        var controller = new MiraPrivatePrototypeController(service, new InternalApiAuthorizer("local-internal"));
        assertThatThrownBy(() -> controller.readingEvidence(null, 1)).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> controller.readingEvidence("wrong", 1)).isInstanceOf(SecurityException.class);
        verifyNoInteractions(service);
        controller.readingEvidence("local-internal", 1);
        verify(service).readingEvidence(1);
    }

    /** Exercita o binding HTTP mesmo quando a compilação não preserva nomes de parâmetros. */
    @Test
    void bindsReadingNumberThroughActualHttpRoute() throws Exception {
        var service = mock(MiraPrivatePrototypeService.class);
        var controller = new MiraPrivatePrototypeController(service, new InternalApiAuthorizer("local-internal"));
        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get("/api/pde/mira/private/v1/internal/readings/2").header("X-PDE-Internal-Token", "local-internal"))
                .andExpect(status().isOk());
        verify(service).readingEvidence(2);
    }

    /** Protege criação e leitura de sessões sintéticas com a credencial interna do PDE. */
    @Test
    void protectsAgentValidationEndpoints() {
        var service = mock(MiraPrivatePrototypeService.class);
        var controller = new MiraPrivatePrototypeController(service, new InternalApiAuthorizer("local-internal"));
        var request = new MiraPrivatePrototypeService.AgentSessionRequest(
                "product:10@agent-validation-v1", "ADHERENT");

        assertThatThrownBy(() -> controller.startAgentValidation(null, request))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> controller.agentValidationEvidence("wrong", "evidence-1"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(service);

        controller.startAgentValidation("local-internal", request);
        controller.agentValidationEvidence("local-internal", "evidence-1");
        verify(service).startAgentValidation(request);
        verify(service).agentValidationEvidence("evidence-1");
    }
}
