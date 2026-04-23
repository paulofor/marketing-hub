package com.marketinghub.mois.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
import com.marketinghub.mois.service.MoisDomainService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MoisDomainController.class)
class MoisDomainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MoisDomainService service;

    @Test
    void shouldCreateDiscoveryRequest() throws Exception {
        when(service.createDiscoveryRequest(any())).thenReturn(
                new MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse("mois-req-1", "ACCEPTED"));

        mockMvc.perform(post("/api/v1/mois/discovery-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nicheName": "Fisioterapia",
                                  "marketTheme": "Dor lombar"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value("mois-req-1"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void shouldListOffers() throws Exception {
        when(service.listOffers(eq("mois-req-1"), eq(null), eq(null))).thenReturn(
                new MoisOfferDtos.OfferCardListResponse(List.of(new MoisOfferDtos.OfferCardSummaryResponse(
                        "mois-offer-1",
                        "mois-req-1",
                        "Fisioterapia",
                        "Oferta Teste",
                        "seller",
                        "promessa",
                        "DIGITAL_PRODUCT",
                        "R$97",
                        0.7))));

        mockMvc.perform(get("/api/v1/mois/offers").param("requestId", "mois-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].offerId").value("mois-offer-1"))
                .andExpect(jsonPath("$.items[0].requestId").value("mois-req-1"));
    }

    @Test
    void shouldReturnNotFoundWhenRequestDoesNotExist() throws Exception {
        when(service.getDiscoveryRequest("mois-req-missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/mois/discovery-requests/mois-req-missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnInsightReport() throws Exception {
        when(service.getInsightReport("mois-report-mois-req-1")).thenReturn(Optional.of(
                new com.marketinghub.mois.dto.MoisInsightDtos.InsightReportResponse(
                        "mois-report-mois-req-1",
                        "mois-req-1",
                        "Fisioterapia",
                        "Dor lombar",
                        "DRAFT",
                        Instant.parse("2026-04-23T10:15:30Z"),
                        new com.marketinghub.mois.dto.MoisInsightDtos.InsightReportRequestSummary(
                                "mois-req-1",
                                "Fisioterapia",
                                "Dor lombar",
                                "Alívio de dor",
                                "COLLECTED",
                                Instant.parse("2026-04-23T10:15:30Z"),
                                Instant.parse("2026-04-23T10:20:30Z")),
                        List.of("mois-offer-1"),
                        List.of(new com.marketinghub.mois.dto.MoisInsightDtos.InsightReportPatternResponse("Promessa", 1, 1.0)),
                        List.of(new com.marketinghub.mois.dto.MoisInsightDtos.InsightReportPatternResponse("Prova", 1, 1.0)),
                        List.of(new com.marketinghub.mois.dto.MoisInsightDtos.InsightReportPatternResponse("R$97", 1, 1.0)),
                        List.of(new com.marketinghub.mois.dto.MoisInsightDtos.InsightReportPatternResponse("Funnel", 1, 1.0)),
                        List.of(new com.marketinghub.mois.dto.MoisInsightDtos.InsightReportPatternResponse("Mecanismo", 1, 1.0)),
                        List.of("Saturação"),
                        List.of(new com.marketinghub.mois.dto.MoisInsightDtos.GapOpportunityResponse(
                                "PROMISE_DIFFERENTIATION",
                                "Gap",
                                "Motivo",
                                List.of("mois-offer-1"),
                                "HIGH",
                                0.7)),
                        List.of("Diferenciação"),
                        List.of("Next"))));

        mockMvc.perform(get("/api/v1/mois/insight-reports/mois-report-mois-req-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value("mois-report-mois-req-1"));
    }
}
