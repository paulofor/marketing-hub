package com.marketinghub.product.service.agentvalidation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.agenttask.AgentTaskTargetContextProvider;
import com.marketinghub.agenttask.AgentTaskTargetResponse;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.businessprocesschain.learningcycle.v1.service.LearningCycleExecutionContext;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Responsabilidade: impedir aprovação de ciclo alheio, encerrado ou de outra versão. */
class PdeAgentValidationCycleContractTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final LearningCycleExecutionContext executions =
      mock(LearningCycleExecutionContext.class);
  private final AgentTaskTargetContextProvider targets = mock(AgentTaskTargetContextProvider.class);
  private final PdeAgentValidationCycleContract resolver =
      new PdeAgentValidationCycleContract(cycles, executions, targets);
  private final Product product = Product.builder().id(4L).slug("vega").build();
  private final BusinessProcessDefinition process = new BusinessProcessDefinition();
  private final LearningSalesCycle cycle = new LearningSalesCycle();
  private com.fasterxml.jackson.databind.node.ObjectNode context;

  /** Monta a identidade do ciclo e a mesma aceitação entregue à fila dos agentes. */
  @BeforeEach
  void setup() throws Exception {
    process.setProcessCode("pde-construction-approval");
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setStatus("OPEN");
    cycle.setStage("ADJUSTMENT");
    cycle.setProductVersion("v12");
    context =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            new ObjectMapper()
                .readTree(
                    """
        {"lineage":{"learningCycleId":2,"productId":4,"experimentId":92},
         "privatePrototypeAcceptance":{"status":"READY","prototypeVersion":"v12", "privateAccessUrl":"https://vega.invalid/private"}}
        """);
    when(cycles.findByExperimentId(92L)).thenReturn(Optional.of(cycle));
    when(executions.source(2L, product, process, true)).thenReturn("experiment:92");
    when(targets.resolve("experiment:92", process.getProcessCode()))
        .thenReturn(
            Optional.of(
                new AgentTaskTargetResponse(
                    "experiment:92",
                    92L,
                    4L,
                    "vega",
                    "Vega",
                    "Vega",
                    "v12",
                    "https://vega.invalid/private",
                    null,
                    null,
                    null,
                    null,
                    context)));
  }

  /** Aceita a passagem exata sem reescrever cadastro, ciclo ou evidências. */
  @Test
  void resolvesExactCycle() {
    assertThat(resolver.resolve(product, process, "experiment:92")).isSameAs(context);
    verify(cycles, never()).save(any());
  }

  /** Rejeita produto diferente, ciclo fechado e tentativa de usar versão anterior. */
  @Test
  void rejectsForeignClosedAndStaleCycle() {
    cycle.setProductId(10L);
    assertThatThrownBy(() -> resolver.resolve(product, process, "experiment:92"))
        .isInstanceOf(IllegalArgumentException.class);
    cycle.setProductId(4L);
    cycle.setStatus("ADJUSTED");
    assertThatThrownBy(() -> resolver.resolve(product, process, "experiment:92"))
        .isInstanceOf(IllegalArgumentException.class);
    cycle.setStatus("OPEN");
    cycle.setProductVersion("v11");
    assertThatThrownBy(() -> resolver.resolve(product, process, "experiment:92"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** Bloqueia linhagem divergente, mesmo quando título e endereço parecem corretos. */
  @Test
  void rejectsAnotherLineage() {
    ((com.fasterxml.jackson.databind.node.ObjectNode) context.path("lineage"))
        .put("learningCycleId", 3);
    assertThatThrownBy(() -> resolver.resolve(product, process, "experiment:92"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
