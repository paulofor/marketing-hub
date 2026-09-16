package com.marketinghub.catalogovivo.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessDefinition;
import com.marketinghub.businessprocess.automation.v1.service.ProcessRunContext;
import com.marketinghub.businessprocess.automation.v1.service.commands.ProcessRunCommand;
import com.marketinghub.businessprocess.execution.service.BusinessProcessActivityExecutionService;
import com.marketinghub.businessprocess.execution.service.productProcessExecutions.ProductProcessActivityExecutionHistoryResponse;
import com.marketinghub.businessprocesschain.*;
import com.marketinghub.businessprocesschain.learningcycle.v1.LearningSalesCycle;
import com.marketinghub.catalogovivo.v1.service.adoption.OpalaAdoption;
import com.marketinghub.opala.commercial.v1.service.*;
import com.marketinghub.product.Product;
import com.marketinghub.producttype.ProductTypeDefinition;
import com.marketinghub.repository.jdbc.catalogovivo.OpalaAdoptionRepository;
import com.marketinghub.repository.jpa.businessprocess.*;
import com.marketinghub.repository.jpa.businessprocesschain.BusinessProcessChainDefinitionRepository;
import com.marketinghub.repository.jpa.learningcycle.LearningSalesCycleRepository;
import com.marketinghub.repository.jpa.product.ProductRepository;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Responsabilidade: impedir adesão implícita e comprovar navegação/partida na cadeia histórica
 * exata.
 */
class OpalaAdoptionRoutingTest {
  /**
   * Aceita somente a definição fixada na adesão, sem reescrever o grafo ou buscar outro processo.
   */
  @Test
  void resolvesExplicitAdoptionPreservingTheOriginalChain() {
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var products = mock(ProductRepository.class);
    var adoptions = mock(OpalaAdoptionRepository.class);
    var routing =
        new OpalaCommercialRouting(
            processes,
            chains,
            mock(BusinessProcessActivityDefinitionRepository.class),
            mock(
                com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository
                    .class),
            products,
            mock(OpalaCommercialContext.class));
    ReflectionTestUtils.setField(routing, "catalogAdoptions", adoptions);
    var product = new Product();
    product.setId(4L);
    product.setProductTypeDefinition(ProductTypeDefinition.builder().code("PDE").build());
    when(products.findById(4L)).thenReturn(Optional.of(product));
    var cycle = new LearningSalesCycle();
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setExperimentId(92L);
    cycle.setChainDefinitionId(14L);
    var process = new BusinessProcessDefinition();
    process.setId(77L);
    process.setProcessCode(CatalogoVivoService.PROCESS);
    when(processes.findById(77L)).thenReturn(Optional.of(process));
    when(adoptions.find(2L))
        .thenReturn(
            Optional.of(
                new OpalaAdoption(2, 77, 4, 92, "v12", 14, "Operador", "Adesão", Instant.now())));
    assertThat(routing.target(cycle)).isSameAs(process);
    assertThat(cycle.getChainDefinitionId()).isEqualTo(14L);
    verifyNoInteractions(chains);
    verify(processes, never())
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(anyString(), anyString());
  }

  /** O motor real recusa a cadeia antiga sem adesão e aceita a passagem exata após o registro. */
  @Test
  void permitsExecutionOnlyAfterExplicitAdoptionAndRejectsAnotherSource() {
    var activities = mock(BusinessProcessActivityExecutionService.class);
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var cycles = mock(LearningSalesCycleRepository.class);
    var adoptions = mock(OpalaAdoptionRepository.class);
    var context =
        new ProcessRunContext(
            activities,
            processes,
            chains,
            cycles,
            mock(ProductRepository.class),
            new ObjectMapper());
    ReflectionTestUtils.setField(context, "catalogAdoptions", adoptions);
    var process = new BusinessProcessDefinition();
    process.setId(77L);
    process.setProcessCode(CatalogoVivoService.PROCESS);
    process.setExecutionScope("PRODUCT");
    process.setParentProcessCode("pde-sales-delivery-learning");
    process.setDiagramJson("{\"nodes\":[{\"id\":\"start\",\"type\":\"START\"}],\"flows\":[]}");
    when(processes.findById(77L)).thenReturn(Optional.of(process));
    var chain = new BusinessProcessChainDefinition();
    chain.setId(14L);
    when(chains.findById(14L)).thenReturn(Optional.of(chain));
    var cycle = new LearningSalesCycle();
    cycle.setId(2L);
    cycle.setProductId(4L);
    cycle.setChainDefinitionId(14L);
    cycle.setStatus("OPEN");
    when(cycles.findById(2L)).thenReturn(Optional.of(cycle));
    var command = new ProcessRunCommand(14L, 2L, "experiment:92");
    assertThatThrownBy(() -> context.read(4L, 77L, command, true))
        .hasMessageContaining("não pertence");
    when(adoptions.permits(2, 4, 14, 77, "experiment:92")).thenReturn(true);
    var history = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(history.currentExecutionReference()).thenReturn("experiment:92");
    when(history.activities()).thenReturn(List.of());
    when(activities.productProcessExecutions(77L, 4L, 2L, 14L, false)).thenReturn(history);
    assertThat(context.read(4L, 77L, command, true)).isSameAs(history);
    assertThatThrownBy(
            () -> context.read(4L, 77L, new ProcessRunCommand(14L, 2L, "experiment:93"), true))
        .hasMessageContaining("não pertence");
    cycle.setStatus("CLOSED");
    assertThatThrownBy(() -> context.read(4L, 77L, command, true))
        .hasMessageContaining("encerrado");
  }
}
