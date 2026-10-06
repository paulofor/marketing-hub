package com.marketinghub.businessprocesschain.learningcycle.v1.decision;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.businessprocesschain.learningcycle.v1.decision.controller.LearningCycleDecisionController;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.LearningCycleDecisionService;
import com.marketinghub.businessprocesschain.learningcycle.v1.decision.service.LearningCycleSuccessorPreparation;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: impedir que o comando anunciado sem gasto dispare a execução dos agentes. */
class LearningCyclePreparationControllerTest {
  /** Valida o contrato HTTP sem corpo usado pela tela, inclusive após uma aprovação histórica. */
  @Test
  void administrativePreparationNeverStartsExecution() throws Exception {
    var preparation = mock(LearningCycleSuccessorPreparation.class);
    var controller =
        new LearningCycleDecisionController(mock(LearningCycleDecisionService.class), preparation);
    MockMvcBuilders.standaloneSetup(controller)
        .build()
        .perform(
            post(
                "/api/business-process-chains/learning-cycles/v1/products/10/3/decision-proposal/prepare-successor"))
        .andExpect(status().isOk());
    verify(preparation).prepareOnly(10L, 3L);
    verifyNoMoreInteractions(preparation);
  }
}
