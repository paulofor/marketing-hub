package com.marketinghub.epm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.epm.controller.EpmController;
import com.marketinghub.epm.service.EpmService;
import com.marketinghub.epm.service.createFinancialPlan.CreateFinancialPlanRequest;
import com.marketinghub.epm.service.getFinancialPlan.FinancialPlanResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Valida o contrato HTTP mínimo do controller único do módulo EPM.
 */
@WebMvcTest(EpmController.class)
class EpmControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    EpmService epmService;

    /** Verifica se o endpoint de criação de plano aceita payload válido e retorna HTTP 201. */
    @Test
    void shouldCreateFinancialPlanThroughApi() throws Exception {
        when(epmService.createFinancialPlan(any())).thenReturn(new FinancialPlanResponse(1L, "Plano Junho", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 300_000L, 2_000L, 3, 3, FinancialPlanStatus.ACTIVE, null, null, null));
        CreateFinancialPlanRequest request = new CreateFinancialPlanRequest("Plano Junho", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), 300_000L, 2_000L, 3, 3, FinancialPlanStatus.ACTIVE, null);

        mockMvc.perform(post("/api/epm/plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.totalBudgetCents").value(300_000));
    }
}
