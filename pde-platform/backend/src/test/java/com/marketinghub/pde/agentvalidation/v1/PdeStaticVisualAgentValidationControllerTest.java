package com.marketinghub.pde.agentvalidation.v1;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.pde.service.InternalApiAuthorizer;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar a proteção das rotas internas do protótipo visual. */
class PdeStaticVisualAgentValidationControllerTest {
    /** Comprova no roteador HTTP que o slug nomeado chega ao controller. */
    @Test
    void bindsNamedProductSlugOnSessionRoute() throws Exception {
        var service = mock(PdeStaticVisualAgentValidationService.class);
        var controller = new PdeStaticVisualAgentValidationController(
                service, new InternalApiAuthorizer("local-internal"));

        MockMvcBuilders.standaloneSetup(controller)
                .build()
                .perform(post("/api/pde/agent-validation/v1/products/pde-planejado-46/internal/sessions")
                        .header("X-PDE-Internal-Token", "local-internal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourceReference":"product:11@agent-validation-v1",
                                  "scenarioCode":"ADHERENT"
                                }
                                """))
                .andExpect(status().isCreated());
        verify(service)
                .startAgentValidation(
                        "pde-planejado-46",
                        new PdeStaticVisualAgentValidationService.AgentSessionRequest(
                                "product:11@agent-validation-v1", "ADHERENT"));
    }

    /** Exige credencial interna antes de criar sessão ou consultar evidência. */
    @Test
    void protectsInternalSessionAndEvidence() {
        var service = mock(PdeStaticVisualAgentValidationService.class);
        var controller = new PdeStaticVisualAgentValidationController(
                service, new InternalApiAuthorizer("local-internal"));
        var request = new PdeStaticVisualAgentValidationService.AgentSessionRequest(
                "product:11@agent-validation-v1", "ADHERENT");

        assertThatThrownBy(() -> controller.start("pde-planejado-46", null, request))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> controller.evidence("pde-planejado-46", "evidence", "wrong"))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(service);

        controller.start("pde-planejado-46", "local-internal", request);
        controller.evidence("pde-planejado-46", "evidence", "local-internal");
        verify(service).startAgentValidation("pde-planejado-46", request);
        verify(service).evidence("pde-planejado-46", "evidence");
    }
}
