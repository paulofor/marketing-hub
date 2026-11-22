package com.marketinghub.businessprocess.automation.v1.service.continuity;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.marketinghub.businessprocess.automation.v1.controller.ProcessRunController;
import com.marketinghub.businessprocess.automation.v1.service.ProcessRunService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Responsabilidade: comprovar o contrato HTTP de preparação sem iniciar ou retomar agentes. */
class ProcessVersionContinuityControllerTest {
  /** Repassa identidade e prova pelo endpoint e apresenta indisponibilidade recuperável. */
  @Test
  void mapsPreviewAndPreparationWithoutCallingExecution() throws Exception {
    var engine = mock(ProcessRunService.class);
    var controller = new ProcessRunController(engine, "", "");
    var http = MockMvcBuilders.standaloneSetup(controller).build();
    String url =
        "/api/business-processes/601/products/71/automation/v1/continuity?learningCycleId=99";
    http.perform(get(url)).andExpect(status().isServiceUnavailable());
    var continuity = mock(ProcessVersionContinuityService.class);
    ReflectionTestUtils.setField(controller, "continuity", continuity);
    http.perform(get(url)).andExpect(status().isOk());
    verify(continuity).preview(71L, 99L, 601L);
    http.perform(
            post(url)
                .contentType("application/json")
                .content(
                    """
        {"fingerprint":"synthetic-hash","operatorName":"Teste","reason":"Conferido",
         "compatibilityEvidence":{"journey":"proof-1"}}
        """))
        .andExpect(status().isOk());
    verify(continuity)
        .prepare(
            71L,
            99L,
            601L,
            new ProcessVersionContinuityService.Request(
                "synthetic-hash", "Teste", "Conferido", Map.of("journey", "proof-1")));
    verifyNoInteractions(engine);
  }
}
