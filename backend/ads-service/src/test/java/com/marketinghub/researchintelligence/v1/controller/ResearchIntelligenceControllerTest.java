package com.marketinghub.researchintelligence.v1.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.researchintelligence.v1.service.ResearchIntelligenceService;
import com.marketinghub.researchintelligence.v1.service.catalog.ResearchIntelligenceAgentPolicyResponse;
import com.marketinghub.researchintelligence.v1.service.catalog.ResearchIntelligenceCatalogResponse;
import com.marketinghub.researchintelligence.v1.service.select.ResearchIntelligenceCardResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/** Valida o contrato HTTP do catálogo global da Biblioteca de Inteligência. */
@WebMvcTest(ResearchIntelligenceController.class)
class ResearchIntelligenceControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockBean private ResearchIntelligenceService service;

  /** Expõe cartões e políticas sem exigir um projeto específico. */
  @Test
  void shouldExposeGlobalCatalogWithoutProjectId() throws Exception {
    ResearchIntelligenceCardResponse card =
        new ResearchIntelligenceCardResponse(
            "RI1-AAAAAAAAAAAA",
            "video",
            "Gancho audiovisual",
            "O primeiro quadro materializa a dor.",
            "Antecipação visual.",
            "Testar abertura reconhecível.",
            "Evidência externa.",
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 10, 15),
            "Aumentar retenção em três segundos.",
            "Generalização.",
            "Não substitui evento humano.",
            "pesquisas/video/2026-08-31-gancho.md",
            "b".repeat(64),
            "EXTERNAL_RESEARCH");
    ResearchIntelligenceCatalogResponse catalog =
        new ResearchIntelligenceCatalogResponse(
            ResearchIntelligenceService.CONTRACT_VERSION,
            LocalDate.of(2026, 9, 3),
            70,
            68,
            List.of(
                new ResearchIntelligenceAgentPolicyResponse(
                    "videomaker",
                    "Apolo",
                    "Orientar roteiro.",
                    "PRODUCTION_ADVISORY",
                    List.of("video", "prazer-audio-visual"),
                    4)),
            List.of(card),
            List.of("Artigos não comprovam venda."));
    when(service.getCatalog()).thenReturn(catalog);

    mockMvc
        .perform(get("/api/research-intelligence/v1/catalog"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contractVersion").value("HARNESS_RESEARCH_INTELLIGENCE_V1"))
        .andExpect(jsonPath("$.totalCompiledCards").value(70))
        .andExpect(jsonPath("$.activeCards").value(68))
        .andExpect(jsonPath("$.agentPolicies[0].agentName").value("Apolo"))
        .andExpect(jsonPath("$.agentPolicies[0].collections[0]").value("video"))
        .andExpect(jsonPath("$.cards[0].cardId").value("RI1-AAAAAAAAAAAA"))
        .andExpect(jsonPath("$.cards[0].sourcePath").value("pesquisas/video/2026-08-31-gancho.md"));

    verify(service).getCatalog();
  }
}
