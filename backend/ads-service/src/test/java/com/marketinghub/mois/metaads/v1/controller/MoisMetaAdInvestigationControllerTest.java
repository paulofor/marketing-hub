package com.marketinghub.mois.metaads.v1.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdDtos;
import com.marketinghub.mois.metaads.v1.service.MoisMetaAdInvestigationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Protege o contrato administrativo da investigação Meta v1. */
class MoisMetaAdInvestigationControllerTest {

  /** Expõe o gate inicial sem fabricar evidências. */
  @Test
  void shouldCreateInvestigationWithInvestigateGate() throws Exception {
    MoisMetaAdInvestigationService service =
        org.mockito.Mockito.mock(MoisMetaAdInvestigationService.class);
    when(service.create(any()))
        .thenReturn(
            new MoisMetaAdDtos.InvestigationResponse(
                81L,
                "workspace-001",
                "agenda cheia",
                "BR",
                "PENDING",
                "INVESTIGAR",
                List.of(),
                List.of("Aguardar observações reais"),
                MoisMetaAdDtos.EthicalModelingCard.empty(),
                0,
                Instant.parse("2026-08-03T20:00:00Z"),
                Instant.parse("2026-08-03T20:00:00Z")));
    MockMvc mvc =
        MockMvcBuilders.standaloneSetup(new MoisMetaAdInvestigationController(service)).build();

    mvc.perform(
            post("/api/v1/mois/meta-ad-investigations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    new ObjectMapper()
                        .writeValueAsString(
                            new MoisMetaAdDtos.CreateInvestigationRequest(
                                "workspace-001", "agenda cheia", "BR"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.gateDecision").value("INVESTIGAR"))
        .andExpect(jsonPath("$.adsObserved").value(0));
  }
}
