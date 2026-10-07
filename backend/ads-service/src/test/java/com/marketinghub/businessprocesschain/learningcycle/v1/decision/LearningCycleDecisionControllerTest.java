package com.marketinghub.businessprocesschain.learningcycle.v1.decision;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.businessprocesschain.learningcycle.v1.decision.controller.LearningCycleDecisionController;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.prepareAdjustment.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Confere o contrato administrativo da preparação sem permitir orçamento ou decisão implícita. */
class LearningCycleDecisionControllerTest {
  private final LearningCycleDecisionService decisions = mock(LearningCycleDecisionService.class);
  private final LearningCycleSuccessorPreparation preparation =
      mock(LearningCycleSuccessorPreparation.class);
  private static final String PATH =
      "/api/business-process-chains/learning-cycles/v1/products/8010/7106/decision-proposal/adjustment-successor";

  /** A leitura consulta somente a elegibilidade; o POST encaminha versão e revisão válidas. */
  @Test
  void exposesCanonicalPreparationAndValidatesRequest() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(new LearningCycleDecisionController(decisions, preparation))
            .build();
    when(preparation.adjustmentAvailability(8010L, 7106L))
        .thenReturn(new AdjustmentPreparationAvailability(true, "Ajuste válido"));
    mvc.perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true));
    verify(preparation, never()).prepareAdjustmentOnly(any(), any(), any());
    mvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":4,\"productVersion\":\"candidate-v2\"}"))
        .andExpect(status().isOk());
    verify(preparation)
        .prepareAdjustmentOnly(
            8010L, 7106L, new PrepareAdjustmentSuccessorRequest(4, "candidate-v2"));
    mvc.perform(
            post(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":-1,\"productVersion\":\"invalid space\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(decisions);
  }
}
