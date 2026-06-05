package com.marketinghub.leadportal.controller;

import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient;
import com.marketinghub.leadportal.service.ExperimentFunnelTrackingClient.TrackingResult;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o endpoint de compatibilidade que recebe submissões públicas no host do Lead Portal.
 */
@WebMvcTest(PublicLeadPortalSubmissionController.class)
class PublicLeadPortalSubmissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExperimentFunnelTrackingClient trackingClient;

    /**
     * Valida que o contrato canônico publicado pelo GeraLanding é encaminhado ao Marketing Hub.
     */
    @Test
    void registerSubmissionForwardsGeraLandingContract() throws Exception {
        UUID submissionId = UUID.fromString("f04e8042-b50f-493a-9be2-e3d73a761321");
        Instant submittedAt = Instant.parse("2026-06-05T15:03:13.127Z");
        when(trackingClient.registerSubmission(
                eq("exp-37-landing-geralanding"),
                eq(submissionId),
                eq(submittedAt),
                eq(null),
                eq("Paulo Forestieri"),
                eq("paulofore@gmail.com"),
                eq(null)))
                .thenReturn(TrackingResult.FORWARDED);

        mockMvc.perform(post("/api/public/lead-portal/flows/exp-37-landing-geralanding/submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contractVersion":"lead-portal-submission-engagement.v1",
                                  "slug":"exp-37-landing-geralanding",
                                  "submissionId":"f04e8042-b50f-493a-9be2-e3d73a761321",
                                  "submittedAt":"2026-06-05T15:03:13.127Z",
                                  "contato":{"nome":"Paulo Forestieri","email":"paulofore@gmail.com"}
                                }
                                """))
                .andExpect(status().isAccepted());

        verify(trackingClient).registerSubmission(
                eq("exp-37-landing-geralanding"),
                eq(submissionId),
                eq(submittedAt),
                eq(null),
                eq("Paulo Forestieri"),
                eq("paulofore@gmail.com"),
                eq(null));
    }

    /**
     * Valida bloqueio de payload com slug divergente para evitar atribuição incorreta de funil.
     */
    @Test
    void registerSubmissionRejectsDivergentSlug() throws Exception {
        mockMvc.perform(post("/api/public/lead-portal/flows/route-slug/submission")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug":"payload-slug",
                                  "submissionId":"f04e8042-b50f-493a-9be2-e3d73a761321",
                                  "contato":{"nome":"Cliente","email":"cliente@example.com"}
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
