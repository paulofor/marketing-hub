package com.marketinghub.oprm.generalaudience.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedStatus;
import com.marketinghub.oprm.generalaudience.OprmGeneralAudienceSeedType;
import com.marketinghub.oprm.generalaudience.service.OprmGeneralAudienceService;
import com.marketinghub.oprm.generalaudience.service.createSeed.CreateGeneralAudienceSeedRequest;
import com.marketinghub.oprm.generalaudience.service.getSeed.GeneralAudienceSeedResponse;
import com.marketinghub.oprm.generalaudience.service.listSeeds.GeneralAudienceSeedSummaryResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Valida o contrato HTTP da etapa de cadastro manual de sementes de público geral do OPRM. */
@WebMvcTest(OprmGeneralAudienceController.class)
class OprmGeneralAudienceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OprmGeneralAudienceService service;

    /** Verifica se a API lista sementes para seleção operacional. */
    @Test
    void shouldListSeedsThroughApi() throws Exception {
        when(service.listSeeds()).thenReturn(List.of(new GeneralAudienceSeedSummaryResponse(
                1L,
                "Beleza",
                "Agenda, WhatsApp e Instagram",
                "BR",
                "pt-BR",
                OprmGeneralAudienceSeedType.CATEGORY,
                OprmGeneralAudienceSeedStatus.DRAFT,
                Instant.parse("2026-06-10T10:00:00Z"))));

        mockMvc.perform(get("/api/oprm/general-audiences/seeds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Beleza"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    /** Verifica se a API cadastra semente sem acionar campanha ou descoberta automática. */
    @Test
    void shouldCreateSeedThroughApi() throws Exception {
        when(service.createSeed(any())).thenReturn(new GeneralAudienceSeedResponse(
                2L,
                "Beleza",
                "Profissionais autônomas",
                "Agenda, WhatsApp e Instagram",
                "BR",
                "pt-BR",
                OprmGeneralAudienceSeedType.CATEGORY,
                OprmGeneralAudienceSeedStatus.DRAFT,
                "Gerar leads qualificados",
                "Evitar promessa absoluta",
                Instant.parse("2026-06-10T10:00:00Z"),
                Instant.parse("2026-06-10T10:00:00Z")));
        CreateGeneralAudienceSeedRequest request = new CreateGeneralAudienceSeedRequest(
                "Beleza",
                "Profissionais autônomas",
                "Agenda, WhatsApp e Instagram",
                null,
                null,
                OprmGeneralAudienceSeedType.CATEGORY,
                null,
                "Gerar leads qualificados",
                "Evitar promessa absoluta");

        mockMvc.perform(post("/api/oprm/general-audiences/seeds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/oprm/general-audiences/seeds/2"))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Beleza"))
                .andExpect(jsonPath("$.seedType").value("CATEGORY"));
    }
}
