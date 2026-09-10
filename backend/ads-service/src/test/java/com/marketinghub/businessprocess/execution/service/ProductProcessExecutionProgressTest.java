package com.marketinghub.businessprocess.execution.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.execution.controller.BusinessProcessActivityExecutionController;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessExecutionProgressResponse;
import com.marketinghub.repository.jpa.agenttask.AgentTaskRepository;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.experiment.ExperimentRepository;
import com.marketinghub.repository.jpa.planning.CommercialPlanRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsabilidade: validar acompanhamento HTTP leve e segregado sem ler a auditoria de tarefas.
 */
class ProductProcessExecutionProgressTest {
  private final AgentTaskRepository tasks = mock(AgentTaskRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ExperimentRepository experiments = mock(ExperimentRepository.class);
  private final CommercialPlanRepository plans = mock(CommercialPlanRepository.class);

  /** Cria o serviço real com persistência isolada na fronteira. */
  private BusinessProcessActivityExecutionService service() {
    var service = new BusinessProcessActivityExecutionService(processes, tasks, new ObjectMapper());
    ReflectionTestUtils.setField(service, "productRepository", products);
    ReflectionTestUtils.setField(service, "experimentRepository", experiments);
    ReflectionTestUtils.setField(service, "commercialPlanRepository", plans);
    when(processes.existsById(70L)).thenReturn(true);
    when(products.existsById(4L)).thenReturn(true);
    when(experiments.existsByIdAndProductId(92L, 4L)).thenReturn(true);
    when(plans.findIdsByProductId(4L)).thenReturn(List.of(17L));
    return service;
  }

  /** O controller entrega a revisão persistida, inclusive fila vazia, sem carregar prompts. */
  @Test
  void exposesTheSmallProgressContract() throws Exception {
    var mvc =
        MockMvcBuilders.standaloneSetup(new BusinessProcessActivityExecutionController(service()))
            .build();
    when(tasks.findProductProcessExecutionProgress(70L, "experiment:92"))
        .thenReturn(
            List.of(
                new ProductProcessExecutionProgressResponse(
                    378L, "IN_PROGRESS", Instant.parse("2026-09-10T17:51:28Z"))));
    mvc.perform(
            get("/api/business-processes/70/products/4/execution-progress")
                .param("sourceReference", "experiment:92"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].taskId").value(378))
        .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$[0].promptSent").doesNotExist());
    verify(tasks).findProductProcessExecutionProgress(70L, "experiment:92");
    verifyNoMoreInteractions(tasks);
    mvc.perform(get("/api/business-processes/70/products/4/execution-progress"))
        .andExpect(status().isBadRequest());
  }

  /**
   * Referências de outros produtos, prefixos próximos e números inválidos não consultam tarefas.
   */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "experiment:91",
        "experiment:922",
        "experiment:9999999999999999999999999",
        "product:40@agent-validation-v1",
        "product:4@",
        "commercial-plan:177@v1",
        "",
        "invalid"
      })
  void refusesForeignAndInvalidReferences(String source) {
    assertThatThrownBy(() -> service().productExecutionProgress(70L, 4L, source))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("não pertence");
    verifyNoInteractions(tasks);
  }

  /** Contextos privados e planos pertencentes ao produto preservam a referência exata. */
  @ParameterizedTest
  @ValueSource(strings = {"product:4@agent-validation-v1", "commercial-plan:17@v2"})
  void acceptsOwnedReferences(String source) {
    service().productExecutionProgress(70L, 4L, source);
    verify(tasks).findProductProcessExecutionProgress(70L, source);
    verifyNoMoreInteractions(tasks);
  }
}
