package com.marketinghub.leadportal.web;

import com.marketinghub.experiment.funnel.ExperimentFunnelService;
import com.marketinghub.leadportal.dto.LeadPortalSubmissionEngagementContractV1;
import com.marketinghub.leadportal.dto.RegisterLandingPageAnalyticsEventRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa os endpoints públicos do backend que recebem eventos de engajamento do Lead Portal.
 */
@WebMvcTest(LeadPortalFlowEngagementController.class)
class LeadPortalFlowEngagementControllerTest {

    /**
     * Aplicação mínima para carregar somente o slice MVC do controller testado.
     */
    @SpringBootApplication
    static class TestConfig {}

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExperimentFunnelService experimentFunnelService;

    /**
     * Valida que render-complete com payload é encaminhado ao serviço de funil.
     */
    @Test
    void registerRenderCompleteAcceptsPayload() throws Exception {
        mockMvc.perform(post("/api/public/lead-portal/flows/flow-slug/render-complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitorId\":\"visitor-123\"}"))
                .andExpect(status().isOk());

        verify(experimentFunnelService).registerFormRenderCompleted(eq("flow-slug"), eq("visitor-123"), isNull());
    }

    /**
     * Valida que render-complete sem corpo continua aceito para compatibilidade pública.
     */
    @Test
    void registerRenderCompleteAcceptsEmptyBody() throws Exception {
        mockMvc.perform(post("/api/public/lead-portal/flows/flow-slug/render-complete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(experimentFunnelService).registerFormRenderCompleted(eq("flow-slug"), isNull(), isNull());
    }

    /**
     * Valida que submissão pública válida é encaminhada ao serviço de funil.
     */
    @Test
    void registerSubmissionForwardsPayload() throws Exception {
        when(experimentFunnelService.registerFormSubmission(
                eq("flow-slug"),
                any(LeadPortalSubmissionEngagementContractV1.class)))
                .thenReturn(true);
        mockMvc.perform(post("/api/public/lead-portal/flows/flow-slug/submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contractVersion":"lead-portal-submission-engagement.v1",
                                  "slug":"flow-slug",
                                  "submissionId":"abc-123",
                                  "submittedAt":"2024-05-01T12:34:56Z",
                                  "contato":{"nome":"Teste","email":"teste@contato.com"}
                                }
                                """))
                .andExpect(status().isOk());

        verify(experimentFunnelService).registerFormSubmission(
                eq("flow-slug"),
                any(LeadPortalSubmissionEngagementContractV1.class));
    }

    /**
     * Valida que analytics emitido pelos scripts da landing é parseado e encaminhado ao serviço.
     */
    @Test
    void registerPageAnalyticsForwardsRawPayload() throws Exception {
        mockMvc.perform(post("/api/public/lead-portal/flows/flow-slug/page-analytics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eventId":"evt-1",
                                  "eventType":"page_view",
                                  "sessionId":"session-1",
                                  "pageUrl":"https://oportunidadebrasil.shop/api/flows/flow-slug/page",
                                  "operatingSystem":"ios",
                                  "screenWidth":390,
                                  "screenHeight":844,
                                  "loadDurationMs":2400,
                                  "domContentLoadedMs":900,
                                  "firstContentfulPaintMs":700,
                                  "resourceErrorCount":2,
                                  "connectionType":"4g"
                                }
                                """))
                .andExpect(status().isOk());

        verify(experimentFunnelService).registerLandingPageAnalyticsEvent(
                eq("flow-slug"),
                org.mockito.ArgumentMatchers.argThat((RegisterLandingPageAnalyticsEventRequest request) ->
                        "evt-1".equals(request.eventId())
                                && "page_view".equals(request.eventType())
                                && "session-1".equals(request.sessionId())
                                && "ios".equals(request.operatingSystem())
                                && Integer.valueOf(390).equals(request.screenWidth())
                                && Integer.valueOf(844).equals(request.screenHeight())
                                && Long.valueOf(2400).equals(request.loadDurationMs())
                                && Long.valueOf(900).equals(request.domContentLoadedMs())
                                && Long.valueOf(700).equals(request.firstContentfulPaintMs())
                                && Integer.valueOf(2).equals(request.resourceErrorCount())
                                && "4g".equals(request.connectionType())));
    }

}
