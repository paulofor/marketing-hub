package com.marketinghub.oprm.generalaudience.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.oprm.generalaudience.service.OprmGeneralAudienceDiscoveryService;
import com.marketinghub.oprm.generalaudience.service.createHypothesis.CreateGeneralAudienceHypothesisRequest;
import com.marketinghub.oprm.generalaudience.service.createHypothesis.GeneralAudienceHypothesisResponse;
import com.marketinghub.oprm.generalaudience.service.createLeadExperiment.CreateGeneralAudienceLeadExperimentRequest;
import com.marketinghub.oprm.generalaudience.service.createLeadExperiment.GeneralAudienceLeadExperimentResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Valida o contrato HTTP do pipeline de descoberta de públicos gerais do OPRM. */
@WebMvcTest(OprmGeneralAudienceDiscoveryController.class)
class OprmGeneralAudienceDiscoveryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    OprmGeneralAudienceDiscoveryService service;

    /** Verifica se a API cria hipótese específica a partir de ângulo aprovado. */
    @Test
    void shouldCreateHypothesisThroughApi() throws Exception {
        UUID hypothesisId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(service.createHypothesis(any(), any())).thenReturn(new GeneralAudienceHypothesisResponse(
                20L,
                5L,
                99L,
                hypothesisId,
                "Hipótese Público Geral - Manicure autônoma",
                "BACKLOG",
                "Acreditamos que manicure autônoma com agenda vazia responderá melhor a uma isca específica.",
                Instant.parse("2026-06-10T13:00:00Z")));
        CreateGeneralAudienceHypothesisRequest request = new CreateGeneralAudienceHypothesisRequest(null, null, null);

        mockMvc.perform(post("/api/oprm/general-audiences/pain-angles/20/create-hypothesis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/hypotheses/" + hypothesisId))
                .andExpect(jsonPath("$.painAngleId").value(20))
                .andExpect(jsonPath("$.marketNicheId").value(99))
                .andExpect(jsonPath("$.status").value("BACKLOG"));
    }

    /** Verifica se a API cria experimento planejado de lead/isca para público geral. */
    @Test
    void shouldCreateLeadExperimentThroughApi() throws Exception {
        UUID hypothesisId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(service.createLeadExperiment(any(), any())).thenReturn(new GeneralAudienceLeadExperimentResponse(
                20L,
                5L,
                99L,
                77L,
                "Lead Público Geral - Manicure autônoma",
                "PLANNED",
                "CPL de lead qualificado",
                new BigDecimal("12.00"),
                new BigDecimal("30.00"),
                LocalDate.parse("2026-06-10"),
                LocalDate.parse("2026-06-12")));
        CreateGeneralAudienceLeadExperimentRequest request = new CreateGeneralAudienceLeadExperimentRequest(
                hypothesisId,
                null,
                "CPL de lead qualificado",
                new BigDecimal("12.00"),
                new BigDecimal("30.00"),
                3,
                new BigDecimal("8.00"),
                30);

        mockMvc.perform(post("/api/oprm/general-audiences/pain-angles/20/create-lead-experiment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/experiments/77"))
                .andExpect(jsonPath("$.experimentId").value(77))
                .andExpect(jsonPath("$.marketNicheId").value(99))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

}
