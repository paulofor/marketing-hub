package com.marketinghub.businessprocesschain.learningcycle.v1.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Comprova o contrato administrativo de prova privada sem confundir registro com decisão comercial.
 */
class LearningCyclePrototypeControllerTest {
  /** Encaminha apenas o registro válido ao serviço canônico, sem chamar comandos de avanço. */
  @Test
  void delegatesProofToCanonicalService() throws Exception {
    var service = mock(LearningCycleService.class);
    MockMvcBuilders.standaloneSetup(new LearningCycleController(service))
        .build()
        .perform(
            post("/api/business-process-chains/learning-cycles/v1/products/9010/8006/private-prototype")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"requestKey":"71c7d0de-3a42-434a-9cb8-d4868a688c8a","expectedRevision":0,
                     "operatorName":"Operador sintético","privatePrototype":{"prototypeVersion":"declared-version-17"}}
                    """))
        .andExpect(status().isOk());
    verify(service).registerPrototype(eq(9010L), eq(8006L), any());
    verifyNoMoreInteractions(service);
  }

  /** Rejeita a prova incompleta na fronteira HTTP antes de qualquer escrita ou execução. */
  @Test
  void rejectsMissingProofAndIdentity() throws Exception {
    var service = mock(LearningCycleService.class);
    MockMvcBuilders.standaloneSetup(new LearningCycleController(service))
        .build()
        .perform(
            post("/api/business-process-chains/learning-cycles/v1/products/9010/8006/private-prototype")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":-1,\"operatorName\":\"\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
