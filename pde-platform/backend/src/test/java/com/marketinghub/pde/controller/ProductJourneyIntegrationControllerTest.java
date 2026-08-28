package com.marketinghub.pde.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.pde.dto.ProductJourneyIntegrationContractResponse;
import com.marketinghub.pde.service.ProductJourneyIntegrationContractService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: validar a rota pública do contrato de integração comercial do PDE. */
class ProductJourneyIntegrationControllerTest {

    /** Preserva o slug da rota e entrega os campos usados pelo gate do Marketing Hub. */
    @Test
    void exposesJourneyIntegrationByProductSlug() throws Exception {
        ProductJourneyIntegrationContractService service =
                mock(ProductJourneyIntegrationContractService.class);
        when(service.get("kit-whatsapp-pronto"))
                .thenReturn(
                        new ProductJourneyIntegrationContractResponse(
                                "kit-whatsapp-pronto",
                                "kit-whatsapp-pronto-pde-v2",
                                "PDE_COMMERCIAL_JOURNEY_EVENTS_V1",
                                "/api/pde/access/events",
                                "/api/pde/access/analytics/{productSlug}/summary",
                                "/api/pde/access/login-link",
                                "/api/pde/access/{token}/workspace",
                                "/api/pde/access/{token}/missions/{missionId}/complete",
                                List.of("PAGE_VIEW", "PURCHASE_COMPLETED", "FIRST_USE"),
                                List.of("eventId", "sessionId"),
                                "pde_funnel_event",
                                "trafficQuality=INTERNAL_QA"));
        MockMvc mvc =
                MockMvcBuilders.standaloneSetup(new ProductJourneyIntegrationController(service)).build();

        mvc.perform(get("/api/pde/products/kit-whatsapp-pronto/integration-contract"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productSlug").value("kit-whatsapp-pronto"))
                .andExpect(jsonPath("$.contractVersion").value("PDE_COMMERCIAL_JOURNEY_EVENTS_V1"))
                .andExpect(jsonPath("$.eventsPath").value("/api/pde/access/events"))
                .andExpect(jsonPath("$.sourceOfTruth").value("pde_funnel_event"));
    }
}
