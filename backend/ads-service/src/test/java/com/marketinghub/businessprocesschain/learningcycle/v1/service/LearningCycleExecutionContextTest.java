package com.marketinghub.businessprocesschain.learningcycle.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainDefinition;
import com.marketinghub.businessprocesschain.BusinessProcessChainItem;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.product.Product;
import com.marketinghub.repository.jpa.businessprocess.BusinessProcessDefinitionRepository;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** Responsabilidade: impedir mistura de experimentos e revalidação de versão alheia ao ciclo. */
class LearningCycleExecutionContextTest {
  private final LearningSalesCycleRepository cycles = mock(LearningSalesCycleRepository.class);
  private final BusinessProcessChainDefinitionRepository chains =
      mock(BusinessProcessChainDefinitionRepository.class);
  private final BusinessProcessDefinitionRepository processes =
      mock(BusinessProcessDefinitionRepository.class);
  private final LearningCycleExecutionContext context =
      new LearningCycleExecutionContext(
          cycles, chains, processes, new LearningCycleJson(new ObjectMapper()));
  private Product product;
  private LearningSalesCycle cycle;
  private BusinessProcessDefinition process;

  /** Monta a identidade de uma iteração independente da seleção legada do plano comercial. */
  @BeforeEach
  void setup() {
    product =
        Product.builder()
            .id(4L)
            .validationDefinitionVersion("PDE_AGENT_VALIDATED_V1")
            .validationDefinitionJson(
                "{\"privatePrototypeAcceptance\":{\"prototypeVersion\":\"v8\"}}")
            .build();
    cycle = new LearningSalesCycle();
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(12L);
    cycle.setStatus("OPEN");
    cycle.setStage("VALIDATION");
    cycle.setProductVersion("v8");
    process = new BusinessProcessDefinition();
    process.setId(70L);
    process.setProcessCode("pde-construction-approval");
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(process);
    var chain = new BusinessProcessChainDefinition();
    chain.getItems().add(item);
    when(cycles.findById(1L)).thenReturn(Optional.of(cycle));
    when(cycles.findByProductIdAndOpenSlot(4L, 1)).thenReturn(List.of(cycle));
    when(chains.findById(12L)).thenReturn(Optional.of(chain));
  }

  /** Preserva o contrato privado e fixa as demais tarefas no experimento da iteração. */
  @Test
  void selectsExactSourceByCanonicalContract() {
    assertThat(context.source(1L, product, process, true))
        .isEqualTo("product:4@agent-validation-v1");
    process.setProcessCode("operacao-otimizacao-experimento");
    assertThat(context.source(1L, product, process, true)).isEqualTo("experiment:92");
    assertThat(context.permitsRevalidation(product)).isTrue();
  }

  /** Bloqueia outro produto, processo externo e comandos em ciclos encerrados. */
  @Test
  void rejectsForeignScopeAndClosedCommands() {
    assertThatThrownBy(() -> context.source(1L, Product.builder().id(5L).build(), process, true))
        .isInstanceOf(ResponseStatusException.class);
    var external = new BusinessProcessDefinition();
    external.setId(99L);
    assertThatThrownBy(() -> context.source(1L, product, external, false))
        .isInstanceOf(ResponseStatusException.class);
    cycle.setStatus("ADJUSTED");
    assertThatThrownBy(() -> context.source(1L, product, process, true))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(context.source(1L, product, process, false))
        .isEqualTo("product:4@agent-validation-v1");
  }

  /** Exige versão exata, etapa de correção e uma única iteração para renovar a homologação. */
  @Test
  void revalidationNeverBypassesCycleIdentity() {
    cycle.setProductVersion("v9");
    assertThat(context.permitsRevalidation(product)).isFalse();
    cycle.setProductVersion("v8");
    cycle.setStage("MEASUREMENT");
    assertThat(context.permitsRevalidation(product)).isFalse();
    cycle.setStage("VALIDATION");
    when(cycles.findByProductIdAndOpenSlot(4L, 1)).thenReturn(List.of(cycle, cycle));
    assertThat(context.permitsRevalidation(product)).isFalse();
  }

  /** Reconhece subprocessos pela composição oficial sem aceitar outra versão de processo raiz. */
  @Test
  void resolvesPublishedSubprocessWithoutGuessing() {
    var child = new BusinessProcessDefinition();
    child.setId(71L);
    child.setProcessCode("child");
    child.setParentProcessCode(process.getProcessCode());
    when(processes.findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            process.getProcessCode(), "PUBLISHED"))
        .thenReturn(Optional.of(process));
    assertThat(context.source(1L, product, child, false)).isEqualTo("experiment:92");
  }
}
