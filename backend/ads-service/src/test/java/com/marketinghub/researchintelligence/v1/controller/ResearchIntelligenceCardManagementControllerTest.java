package com.marketinghub.researchintelligence.v1.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.researchintelligence.v1.ResearchIntelligenceCardStatus;
import com.marketinghub.researchintelligence.v1.ResearchIntelligenceSourceKind;
import com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceCardManagementService;
import com.marketinghub.researchintelligence.v1.service.managecard.RegisterResearchIntelligenceCardRequest;
import com.marketinghub.researchintelligence.v1.service.managecard.ResearchIntelligenceCardVersionResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Valida a superfície HTTP interna e assinada da gestão editorial de cartões. */
@WebMvcTest(ResearchIntelligenceCardManagementController.class)
class ResearchIntelligenceCardManagementControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockBean private ResearchIntelligenceCardManagementService service;

  /** Encaminha cabeçalhos, JSON e autoria antes de devolver a nova localização canônica. */
  @Test
  void shouldRegisterSignedCardVersion() throws Exception {
    when(service.registerCard(
            any(RegisterResearchIntelligenceCardRequest.class),
            eq("codex-homologacao"),
            eq("idem-register-001")))
        .thenReturn(response());

    mockMvc
        .perform(
            post("/api/internal/research-intelligence/v1/cards")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Actor", "codex-homologacao")
                .header("Idempotency-Key", "idem-register-001")
                .header("X-Harness-Request-Id", "f4c0710f-04f2-41b7-91eb-04bd74f95bf4")
                .content(validJson()))
        .andExpect(status().isCreated())
        .andExpect(
            header()
                .string(
                    "Location",
                    "/api/internal/research-intelligence/v1/cards/homologacao-card/versions/1"))
        .andExpect(jsonPath("$.cardKey").value("homologacao-card"))
        .andExpect(jsonPath("$.version").value(1))
        .andExpect(jsonPath("$.status").value("DRAFT"));

    verify(service)
        .verifyInternalRequest(
            any(),
            eq("codex-homologacao"),
            eq("idem-register-001"),
            any(RegisterResearchIntelligenceCardRequest.class));
    verify(service)
        .registerCard(
            any(RegisterResearchIntelligenceCardRequest.class),
            eq("codex-homologacao"),
            eq("idem-register-001"));
  }

  /** Monta o JSON completo aceito pelo contrato de cadastro interno. */
  private String validJson() {
    return """
        {
          "cardKey": "homologacao-card",
          "collection": "video",
          "title": "Demonstração clara",
          "finding": "Mostrar reduz ambiguidade.",
          "mechanism": "Concretização visual.",
          "commercialApplication": "Comparar retenção e CTA.",
          "evidenceStrength": "Hipótese externa.",
          "publishedOn": "2026-09-04",
          "validUntil": "2026-10-19",
          "experimentHypothesis": "A demonstração aumentará CTA.",
          "risks": "Generalização.",
          "limits": "Pagamento comprova venda.",
          "sourceKind": "TEXT",
          "sourceUri": "urn:test:card",
          "sourceTitle": "Fonte sintética",
          "sourceSha256": "%s"
        }
        """
        .formatted("a".repeat(64));
  }

  /** Monta a versão mínima retornada pela gestão após persistência bem-sucedida. */
  private ResearchIntelligenceCardVersionResponse response() {
    LocalDateTime now = LocalDateTime.of(2026, 9, 4, 12, 0);
    return new ResearchIntelligenceCardVersionResponse(
        "homologacao-card",
        1,
        "RI1-CCCCCCCCCCCC",
        ResearchIntelligenceCardStatus.DRAFT,
        "DRAFT",
        "video",
        List.of("videomaker"),
        "Demonstração clara",
        "Mostrar reduz ambiguidade.",
        "Concretização visual.",
        "Comparar retenção e CTA.",
        "Hipótese externa.",
        LocalDate.of(2026, 9, 4),
        LocalDate.of(2026, 10, 19),
        "A demonstração aumentará CTA.",
        "Generalização.",
        "Pagamento comprova venda.",
        ResearchIntelligenceSourceKind.TEXT,
        "urn:test:card",
        "Fonte sintética",
        "a".repeat(64),
        "codex-homologacao",
        now,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        now);
  }
}
