package com.marketinghub.businessprocessresource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.businessprocessresource.controller.BusinessProcessExecutionResourceController;
import com.marketinghub.businessprocessresource.service.BusinessProcessExecutionResourceService;
import com.marketinghub.businessprocessresource.service.listResources.BusinessProcessExecutionResourceResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar o contrato HTTP do catálogo de recursos de atividades. */
class BusinessProcessExecutionResourceControllerTest {

  /** Retorna código, agente e executor oficiais sem inferência do frontend. */
  @Test
  void listsExecutionResources() throws Exception {
    var service = mock(BusinessProcessExecutionResourceService.class);
    when(service.listResources())
        .thenReturn(
            List.of(
                new BusinessProcessExecutionResourceResponse(
                    1L,
                    "themis-image-studio",
                    "Materializador visual técnico de Dédalo",
                    "Cria e edita imagens.",
                    "CONTAINER",
                    "landing-generator",
                    "themis-image-studio",
                    "Use o pending do backend.")));
    var mockMvc =
        MockMvcBuilders.standaloneSetup(new BusinessProcessExecutionResourceController(service))
            .build();

    mockMvc
        .perform(get("/api/business-process-execution-resources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].resourceCode").value("themis-image-studio"))
        .andExpect(jsonPath("$[0].responsibleAgentKey").value("landing-generator"))
        .andExpect(jsonPath("$[0].executorReference").value("themis-image-studio"));
  }
}
