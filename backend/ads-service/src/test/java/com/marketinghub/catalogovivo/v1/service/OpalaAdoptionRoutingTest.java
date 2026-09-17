package com.marketinghub.catalogovivo.v1.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketinghub.businessprocess.BusinessProcessActivityDefinition;
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

  /**
   * Consulta a conclusão pela projeção sem lock para permanecer compatível com a tela somente
   * leitura no MySQL.
   */
  @Test
  void readsCompletionWithoutRequestingWriteLock() {
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var activities = mock(BusinessProcessActivityDefinitionRepository.class);
    var instances =
        mock(
            com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository
                .class);
    var products = mock(ProductRepository.class);
    var adoptions = mock(OpalaAdoptionRepository.class);
    var routing =
        new OpalaCommercialRouting(
            processes, chains, activities, instances, products, mock(OpalaCommercialContext.class));
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
    cycle.setProductVersion("musa-pde-entry-v12-primeiro-ajuste-aplicavel");
    cycle.setStatus("OPEN");
    cycle.setStage("PUBLICATION");
    var process = new BusinessProcessDefinition();
    process.setId(77L);
    when(processes.findById(77L)).thenReturn(Optional.of(process));
    when(adoptions.find(2L))
        .thenReturn(
            Optional.of(
                new OpalaAdoption(2, 77, 4, 92, "v12", 14, "Operador", "Adesão", Instant.now())));
    var ready = new BusinessProcessActivityDefinition();
    ready.setId(501L);
    when(activities.findByProcessDefinitionIdAndActivityId(77L, "ready"))
        .thenReturn(Optional.of(ready));
    when(instances.findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            501L, "experiment:92"))
        .thenReturn(Optional.empty());

    assertThat(routing.completed(cycle)).isFalse();
    verify(instances)
        .findFirstByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            501L, "experiment:92");
    verify(instances, never())
        .findTopByActivityDefinitionIdAndSourceReferenceOrderByOccurrenceNumberDesc(
            anyLong(), anyString());
  }

  /** Encontra Opala na atividade tipada do Processo 5 sem depender do Processo 6 legado. */
  @Test
  void resolvesTypedRouteFromProcessFive() throws Exception {
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var products = mock(ProductRepository.class);
    var context = mock(OpalaCommercialContext.class);
    var routing =
        new OpalaCommercialRouting(
            processes,
            chains,
            mock(BusinessProcessActivityDefinitionRepository.class),
            mock(
                com.marketinghub.repository.jpa.agenttask.BusinessProcessActivityInstanceRepository
                    .class),
            products,
            context);
    var product = new Product();
    product.setId(4L);
    product.setProductTypeDefinition(ProductTypeDefinition.builder().code("PDE").build());
    when(products.findById(4L)).thenReturn(Optional.of(product));
    var cycle = new LearningSalesCycle();
    cycle.setProductId(4L);
    cycle.setChainDefinitionId(16L);
    var processFive = new BusinessProcessDefinition();
    processFive.setId(56L);
    processFive.setProcessCode("pde-commercial-homologation-activation");
    processFive.setDiagramJson("process-five-v7");
    var chain = new BusinessProcessChainDefinition();
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(processFive);
    chain.getItems().add(item);
    when(chains.findById(16L)).thenReturn(Optional.of(chain));
    when(context.read("process-five-v7"))
        .thenReturn(
            new ObjectMapper()
                .readTree(
                    "{\"nodes\":[{\"id\":\"commercialPreparation\",\"type\":\"TASK\",\"subprocessRoutes\":[{\"productTypeCode\":\"PDE\",\"subprocessCode\":\"opala-commercial-preparation-v1\",\"subprocessVersion\":1}]}]}"));
    var target = new BusinessProcessDefinition();
    target.setId(77L);
    target.setProcessCode("opala-commercial-preparation-v1");
    target.setVersionNumber(1);
    target.setStatus("PUBLISHED");
    when(processes.findByProcessCodeAndVersionNumber("opala-commercial-preparation-v1", 1))
        .thenReturn(Optional.of(target));

    assertThat(routing.target(cycle)).isSameAs(target);
    verify(processes, never())
        .findFirstByProcessCodeAndStatusOrderByVersionNumberDesc(
            "opala-commercial-preparation-v1", "PUBLISHED");
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

  /** Reconhece o subprocesso tipado como membro real da cadeia nova sem adesão excepcional. */
  @Test
  void permitsTypedSubprocessDeclaredByChainGraph() {
    var activities = mock(BusinessProcessActivityExecutionService.class);
    var processes = mock(BusinessProcessDefinitionRepository.class);
    var chains = mock(BusinessProcessChainDefinitionRepository.class);
    var cycles = mock(LearningSalesCycleRepository.class);
    var products = mock(ProductRepository.class);
    var context =
        new ProcessRunContext(activities, processes, chains, cycles, products, new ObjectMapper());
    var product = new Product();
    product.setId(4L);
    product.setProductTypeDefinition(ProductTypeDefinition.builder().code("PDE").build());
    when(products.findById(4L)).thenReturn(Optional.of(product));
    var target = new BusinessProcessDefinition();
    target.setId(77L);
    target.setProcessCode("opala-commercial-preparation-v1");
    target.setVersionNumber(1);
    target.setExecutionScope("PRODUCT");
    target.setDiagramJson("{\"nodes\":[{\"id\":\"start\",\"type\":\"START\"}],\"flows\":[]}");
    var parent = new BusinessProcessDefinition();
    parent.setId(56L);
    parent.setProcessCode("pde-commercial-homologation-activation");
    parent.setDiagramJson(
        "{\"nodes\":[{\"id\":\"commercialPreparation\",\"type\":\"TASK\",\"subprocessRoutes\":[{\"productTypeCode\":\"PDE\",\"subprocessCode\":\"opala-commercial-preparation-v1\",\"subprocessVersion\":1}]}],\"flows\":[]}");
    when(processes.findById(77L)).thenReturn(Optional.of(target));
    when(processes.findById(56L)).thenReturn(Optional.of(parent));
    var chain = new BusinessProcessChainDefinition();
    chain.setId(16L);
    var item = new BusinessProcessChainItem();
    item.setProcessDefinition(parent);
    chain.getItems().add(item);
    when(chains.findById(16L)).thenReturn(Optional.of(chain));
    var cycle = new LearningSalesCycle();
    cycle.setId(3L);
    cycle.setProductId(4L);
    cycle.setChainDefinitionId(16L);
    cycle.setStatus("OPEN");
    when(cycles.findById(3L)).thenReturn(Optional.of(cycle));
    var history = mock(ProductProcessActivityExecutionHistoryResponse.class);
    when(history.currentExecutionReference()).thenReturn("experiment:93");
    when(history.activities()).thenReturn(List.of());
    when(activities.productProcessExecutions(77L, 4L, 3L, 16L, false)).thenReturn(history);

    assertThat(context.read(4L, 77L, new ProcessRunCommand(16L, 3L, "experiment:93"), true))
        .isSameAs(history);

    product.setProductTypeDefinition(ProductTypeDefinition.builder().code("OUTRO").build());
    assertThatThrownBy(
            () -> context.read(4L, 77L, new ProcessRunCommand(16L, 3L, "experiment:93"), true))
        .hasMessageContaining("não pertence");
  }
}
