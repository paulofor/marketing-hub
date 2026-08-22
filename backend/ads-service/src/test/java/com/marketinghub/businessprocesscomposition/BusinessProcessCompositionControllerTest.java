package com.marketinghub.businessprocesscomposition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.businessprocesscomposition.controller.BusinessProcessCompositionController;
import com.marketinghub.businessprocesscomposition.service.BusinessProcessCompositionService;
import com.marketinghub.businessprocesscomposition.service.getcomposition.BusinessProcessCompositionResponse;
import com.marketinghub.businessprocesscomposition.service.getcomposition.BusinessProcessReferenceResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar o contrato HTTP da composição dos processos. */
class BusinessProcessCompositionControllerTest {

  /** Expõe processo, contagem e subprocessos vigentes em contrato estruturado. */
  @Test
  void getsBusinessProcessComposition() throws Exception {
    var service = mock(BusinessProcessCompositionService.class);
    var parent =
        new BusinessProcessReferenceResponse(
            40L,
            "communication",
            "Comunicação",
            "Integrar a jornada.",
            "Operação",
            4,
            "PUBLISHED",
            "VALUE_PROCESS");
    var child =
        new BusinessProcessReferenceResponse(
            17L,
            "creative-production",
            "Criação de criativos",
            "Produzir criativos.",
            "Operação",
            5,
            "PUBLISHED",
            "SUBPROCESS");
    when(service.getComposition(40L))
        .thenReturn(new BusinessProcessCompositionResponse(parent, null, 1, List.of(child)));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new BusinessProcessCompositionController(service)).build();

    mockMvc
        .perform(get("/api/business-processes/40/composition"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.process.name").value("Comunicação"))
        .andExpect(jsonPath("$.subprocessCount").value(1))
        .andExpect(jsonPath("$.subprocesses[0].name").value("Criação de criativos"));
  }
}
