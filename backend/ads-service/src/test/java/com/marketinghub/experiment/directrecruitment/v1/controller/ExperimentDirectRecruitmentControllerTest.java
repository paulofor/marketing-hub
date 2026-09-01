package com.marketinghub.experiment.directrecruitment.v1.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.experiment.directrecruitment.v1.service.ExperimentDirectRecruitmentService;
import com.marketinghub.experiment.directrecruitment.v1.service.campaign.DirectRecruitmentCampaignResponse;
import com.marketinghub.experiment.directrecruitment.v1.service.submit.SubmitDirectRecruitmentRequest;
import com.marketinghub.experiment.directrecruitment.v1.service.submit.SubmitDirectRecruitmentResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Responsabilidade: validar rotas, formatos e gates HTTP do recrutamento direto. */
@WebMvcTest(ExperimentDirectRecruitmentController.class)
class ExperimentDirectRecruitmentControllerTest {
  private static final String TOKEN = "11111111-2222-4333-8444-555555555555";

  @Autowired private MockMvc mockMvc;
  @MockBean private ExperimentDirectRecruitmentService service;

  /** Expõe o estado persistido da aquisição na rota administrativa canônica. */
  @Test
  void shouldExposeAdministrativeCampaign() throws Exception {
    when(service.getCampaign(89L)).thenReturn(campaign());

    mockMvc
        .perform(get("/api/experiments/89/direct-recruitment"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.experimentId").value(89))
        .andExpect(jsonPath("$.acquisitionStatus").value("ACTIVE_WITHOUT_DISTRIBUTION"))
        .andExpect(jsonPath("$.recordedContacts").value(0));
  }

  /** Aceita uma adesão completa e devolve a oferta apenas do resultado qualificado. */
  @Test
  void shouldAcceptValidPublicSubmission() throws Exception {
    when(service.submit(eq(TOKEN), any(SubmitDirectRecruitmentRequest.class)))
        .thenReturn(
            new SubmitDirectRecruitmentResponse(
                20L, "QUALIFIED", true, "Perfil aderente", "https://rigel.example", 14, false));

    mockMvc
        .perform(
            post("/api/public/direct-recruitments/{token}/submissions", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSubmissionJson(true, "consent-v1")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.qualified").value(true))
        .andExpect(jsonPath("$.offerUrl").value("https://rigel.example"));
  }

  /** Interrompe o request no controller quando o consentimento não foi aceito. */
  @Test
  void shouldRejectSubmissionWithoutConsentBeforeService() throws Exception {
    mockMvc
        .perform(
            post("/api/public/direct-recruitments/{token}/submissions", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSubmissionJson(false, "consent-v1")))
        .andExpect(status().isBadRequest());

    verify(service, never()).submit(any(), any());
  }

  /** Interrompe chaves de submissão que não seguem UUID versionado. */
  @Test
  void shouldRejectMalformedSubmissionKey() throws Exception {
    String body =
        validSubmissionJson(true, "consent-v1")
            .replace(UUID.fromString(TOKEN).toString(), "11111111-2222-0333-0444-555555555555");

    mockMvc
        .perform(
            post("/api/public/direct-recruitments/{token}/submissions", TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());

    verify(service, never()).submit(any(), any());
  }

  /** Monta a resposta administrativa mínima para o contrato HTTP. */
  private DirectRecruitmentCampaignResponse campaign() {
    return new DirectRecruitmentCampaignResponse(
        10L,
        89L,
        "Kit WhatsApp Pronto",
        "ACTIVE",
        "direct-recruitment-v1",
        "Convite",
        "Validação",
        "Prestadores",
        "Aceito participar",
        "consent-v1",
        "https://rigel.example",
        "Conhecer Rigel",
        "https://rigel.example/privacidade",
        "/participar/" + TOKEN,
        15,
        15,
        0,
        0,
        0,
        0,
        0,
        0,
        "ACTIVE_WITHOUT_DISTRIBUTION",
        "Conecte uma conta orgânica.",
        "Operador QA",
        "Operador QA",
        "Ativo sem distribuição",
        null,
        null,
        null,
        null,
        null);
  }

  /** Monta um JSON válido variando apenas o aceite e a versão do consentimento. */
  private String validSubmissionJson(boolean accepted, String consentVersion) {
    return """
        {
          "contactFingerprint": "%s",
          "submissionKey": "%s",
          "serviceSegment": "CONSULTING",
          "weeklyConversationsRange": "ELEVEN_TO_THIRTY",
          "usesWhatsapp": true,
          "decisionMaker": true,
          "wantsPersonalizedImplementation": true,
          "consentAccepted": %s,
          "consentVersion": "%s"
        }
        """
        .formatted("a".repeat(64), TOKEN, accepted, consentVersion);
  }
}
