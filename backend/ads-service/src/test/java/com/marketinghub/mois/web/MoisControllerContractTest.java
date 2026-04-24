package com.marketinghub.mois.web;

import com.marketinghub.mois.dto.MoisDiscoveryDtos;
import com.marketinghub.mois.dto.MoisInsightDtos;
import com.marketinghub.mois.dto.MoisOfferDtos;
import com.marketinghub.mois.service.MoisModuleGateway;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MoisController.class)
class MoisControllerContractTest {

    @SpringBootApplication
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MoisModuleGateway gateway;

    @Test
    void shouldAcceptDiscoveryRequest() throws Exception {
        when(gateway.createDiscoveryRequest(any(MoisDiscoveryDtos.CreateDiscoveryRequest.class)))
                .thenReturn(new MoisDiscoveryDtos.DiscoveryRequestAcceptedResponse("mois-req-123", "ACCEPTED"));

        mockMvc.perform(post("/api/v1/mois/discovery-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nicheName": "personal trainer",
                                  "marketTheme": "retencao de alunos"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").value("mois-req-123"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    @Test
    void shouldReturnValidationErrorWhenRequiredFieldsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/mois/discovery-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnDiscoveryRequestDetail() throws Exception {
        when(gateway.getDiscoveryRequest(eq("mois-req-001")))
                .thenReturn(Optional.of(new MoisDiscoveryDtos.DiscoveryRequestDetailResponse(
                        "mois-req-001",
                        "personal trainer",
                        "retencao de alunos",
                        "agenda previsivel sem desconto",
                        "DRAFT",
                        Instant.parse("2026-04-22T00:00:00Z"),
                        List.of(new MoisDiscoveryDtos.ArtifactRefResponse("mois-art-001", "mois.marketOfferDiscoveryRequest.v1", "v1"))
                )));

        mockMvc.perform(get("/api/v1/mois/discovery-requests/mois-req-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("mois-req-001"))
                .andExpect(jsonPath("$.artifacts[0].artifactId").value("mois-art-001"));
    }

    @Test
    void shouldReturn404WhenOfferDoesNotExist() throws Exception {
        when(gateway.getOffer(eq("unknown-offer"))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/mois/offers/unknown-offer"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnOfferListContract() throws Exception {
        when(gateway.listOffers(any(), any(), any()))
                .thenReturn(new MoisOfferDtos.OfferCardListResponse(List.of(
                        new MoisOfferDtos.OfferCardSummaryResponse(
                                "mois-offer-001",
                                "mois-req-001",
                                "personal trainer",
                                "Agenda Cheia Sem Desconto",
                                "Studio Exemplo",
                                "Agenda previsivel com onboarding estruturado",
                                "mentoria",
                                "R$ 1.497",
                                0.79
                        )
                )));

        mockMvc.perform(get("/api/v1/mois/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].offerId").value("mois-offer-001"))
                .andExpect(jsonPath("$.items[0].confidence").value(0.79));
    }

    @Test
    void shouldPropagateCategoryFilterToInsightReportsEndpoint() throws Exception {
        when(gateway.listInsightReports(eq("mois-req-001"), eq("nutricao"), eq("DIGITAL_PRODUCT")))
                .thenReturn(new MoisInsightDtos.InsightReportListResponse(List.of()));

        mockMvc.perform(get("/api/v1/mois/insight-reports")
                        .param("requestId", "mois-req-001")
                        .param("nicheName", "nutricao")
                        .param("category", "DIGITAL_PRODUCT"))
                .andExpect(status().isOk());
    }
}
